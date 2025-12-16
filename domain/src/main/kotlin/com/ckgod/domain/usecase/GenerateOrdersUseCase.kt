package com.ckgod.domain.usecase

import com.ckgod.domain.model.*
import com.ckgod.domain.repository.AccountRepository
import com.ckgod.domain.repository.InvestmentStatusRepository
import com.ckgod.domain.repository.StockRepository

/**
 * 주문 생성 UseCase
 *
 * 역할:
 * - ticker == null: 전체 종목 주문 생성 (Job용)
 * - ticker != null: 단일 종목 주문 생성 (API/백테스트용)
 */
class GenerateOrdersUseCase(
    private val stockRepository: StockRepository,
    private val accountRepository: AccountRepository,
    private val investmentStatusRepository: InvestmentStatusRepository
) {
    suspend operator fun invoke(ticker: String? = null): List<OrderResult> {
        val targets = if (ticker != null) {
            val status = investmentStatusRepository.get(ticker)
            if (status != null) listOf(status) else emptyList()
        } else {
            investmentStatusRepository.findAll()
        }

        // 각 종목 주문 생성
        return targets.mapNotNull { status ->
            try {
                generateSingle(status)
            } catch (e: Exception) {
                null // 실패한 종목은 제외
            }
        }
    }

    private suspend fun generateSingle(status: InvestmentStatus): OrderResult? {
        // 현재 상태 조회
        val currentStatus = investmentStatusRepository.get(status.ticker) ?: return null

        // 현재 보유 정보 조회
        val holding = accountRepository.getBalance(status.ticker)
        val currentQuantity = holding?.quantity?.toDoubleOrNull()?.toInt() ?: 0
        val currentPrice = holding?.currentPrice?.toDoubleOrNull() ?: 0.0

        // 매수 주문 생성
        val buyOrders = generateBuyOrders(
            status = currentStatus,
            currentPrice = currentPrice
        )

        // 매도 주문 생성
        val sellOrders = generateSellOrders(
            status = currentStatus,
            currentQuantity = currentQuantity,
        )

        // 실제 주문 api 전송
        stockRepository.postOrder(buyOrders, sellOrders)

        // 주문 정보 반환
        return OrderResult(
            ticker = status.ticker,
            currentPrice = currentPrice,
            buyOrders = buyOrders,
            sellOrders = sellOrders
        )
    }

    /**
     * 매수 주문 생성
     *
     * 현재 T값에 따라 매수 분배가 다름
     * 1. 전반전 (0 <= T < division / 2)
     * - 1회 매수액의 절반을 별% LOC로 매수 시도
     * - 1회 매수액의 절반을 평단가(0%) LOC로 매수 시도
     *
     * 2. 후반전 (division / 2 <= T <= division -1)
     * - 1회 매수액 전체를 별% LOC로 매수 시도
     *
     * 전후반 공통으로 크게 하락하는 경우를 대비해, 1회 정액 매수를 맞추기 위해 아래로 LOC 매수를 추가 시도한다.
     */
    private fun generateBuyOrders(
        status: InvestmentStatus,
        currentPrice: Double,
    ): List<OrderRequest> {
        val orders = mutableListOf<OrderRequest>()

        // 별% LOC 매수 가격
        val starBuyPrice = currentPrice * (1.0 + status.starPercent / 100.0)

        when (status.phase) {
            TradePhase.FRONT_HALF -> {
                val halfAmount = status.oneTimeAmount / 2.0

                // 1. 별% LOC 매수 (절반)
                val starBuyQty = (halfAmount / starBuyPrice).toInt()

                if (starBuyQty > 0) {
                    orders.add(OrderRequest(
                        ticker = status.ticker,
                        exchange = status.exchange,
                        side = OrderSide.BUY,
                        type = OrderType.LOC,
                        price = starBuyPrice,
                        quantity = starBuyQty,
                    ))
                }

                // 2. 평단가(0%) LOC 매수 (절반)
                if (status.avgPrice > 0) {
                    val avgBuyQty = (halfAmount / status.avgPrice).toInt()
                    if (avgBuyQty > 0) {
                        orders.add(OrderRequest(
                            ticker = status.ticker,
                            exchange = status.exchange,
                            side = OrderSide.BUY,
                            type = OrderType.LOC,
                            price = status.avgPrice,
                            quantity = avgBuyQty
                        ))
                    }
                }

            }
            TradePhase.BACK_HALF -> {
                // 후반전: 1회 매수액 전체를 별% LOC
                val fullBuyQty = (status.oneTimeAmount / starBuyPrice).toInt()
                if (fullBuyQty > 0) {
                    orders.add(OrderRequest(
                        ticker = status.ticker,
                        exchange = status.exchange,
                        side = OrderSide.BUY,
                        type = OrderType.LOC,
                        price = starBuyPrice,
                        quantity = fullBuyQty
                    ))
                }
            }
            else -> Unit
        }

        // 폭락 대비 추가 LOC 매수
        val crashRates = listOf(0.06, 0.10, 0.12, 0.14, 0.15)
        crashRates.forEach { rate ->
            val crashPrice = currentPrice * (1.0 - rate)
            orders.add(OrderRequest(
                ticker = status.ticker,
                exchange = status.exchange,
                side = OrderSide.BUY,
                type = OrderType.LOC,
                price = crashPrice,
                quantity = 1
            ))
        }

        return orders
    }

    /**
     * 매도 주문 생성
     *
     * T <= division - 1 인 경우 전후반전 상관없이 공통으로 적용
     * - 누적수량의 1/4 분량을 별% LOC 매도 시도
     * - 누적수량의 3/4 분량을 별% 지정가 매도 시도
     *
     * division - 1 < T < division 인 경우 쿼터 손절 기간
     * - 누적 수량의 1/4 분량을 MOC 매도로 걸어서 무조건 매도를 시도 (쿼터 손절)
     * - 누적 수량의 3/4 분량을 별% 지정가 매도 시도
     */
    private fun generateSellOrders(
        status: InvestmentStatus,
        currentQuantity: Int,
    ): List<OrderRequest> {
        if (currentQuantity == 0 || status.avgPrice == 0.0) {
            return emptyList()
        }

        val orders = mutableListOf<OrderRequest>()

        // 별% 매도 가격
        val starSellPrice = status.avgPrice * (1.0 + status.starPercent / 100.0)
        // 목표 지정가 매도 가격
        val targetPrice = status.avgPrice * (1.0 + status.targetRate / 100.0)

        // 수량 계산
        val quarterQty = (currentQuantity / 4.0).toInt()
        val threeQuarterQty = currentQuantity - quarterQty

        when(status.phase) {
            TradePhase.FRONT_HALF, TradePhase.BACK_HALF -> {
                // 일반 매도

                // 1. 1/4 수량을 별% LOC 매도
                if (quarterQty > 0) {
                    orders.add(OrderRequest(
                        ticker = status.ticker,
                        exchange = status.exchange,
                        side = OrderSide.SELL,
                        type = OrderType.LOC,
                        price = starSellPrice,
                        quantity = quarterQty
                    ))
                }

                // 2. 3/4 수량을 목표 지정가 매도
                if (threeQuarterQty > 0) {
                    orders.add(OrderRequest(
                        ticker = status.ticker,
                        exchange = status.exchange,
                        side = OrderSide.SELL,
                        type = OrderType.LIMIT,
                        price = targetPrice,
                        quantity = threeQuarterQty
                    ))
                }
            }
            TradePhase.QUARTER_MODE -> {
                // 1. 1/4 수량을 MOC 매도 (무조건 매도)
                if (quarterQty > 0) {
                    orders.add(OrderRequest(
                        ticker = status.ticker,
                        exchange = status.exchange,
                        side = OrderSide.SELL,
                        type = OrderType.MOC,
                        price = 0.0, // MOC는 가격 불필요
                        quantity = quarterQty
                    ))
                }

                // 2. 3/4 수량을 별% 지정가 매도
                if (threeQuarterQty > 0) {
                    orders.add(OrderRequest(
                        ticker = status.ticker,
                        exchange = status.exchange,
                        side = OrderSide.SELL,
                        type = OrderType.LIMIT,
                        price = targetPrice,
                        quantity = threeQuarterQty
                    ))
                }
            }
            else -> Unit
        }

        return orders
    }

    /**
     * 주문 생성 결과
     */
    data class OrderResult(
        val ticker: String,
        val currentPrice: Double,
        val buyOrders: List<OrderRequest>,
        val sellOrders: List<OrderRequest>
    )
}
