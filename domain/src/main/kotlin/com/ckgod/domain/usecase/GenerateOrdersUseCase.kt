package com.ckgod.domain.usecase

import com.ckgod.domain.model.*
import com.ckgod.domain.repository.AccountRepository
import com.ckgod.domain.repository.InvestmentStatusRepository
import com.ckgod.domain.repository.StockRepository
import org.slf4j.LoggerFactory

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
    private val logger = LoggerFactory.getLogger(GenerateOrdersUseCase::class.java)

    suspend operator fun invoke(ticker: String? = null): List<OrderResult> {
        logger.info("[GenerateOrders] 시작 - ticker: ${ticker ?: "전체"}")

        val targets = if (ticker != null) {
            val status = investmentStatusRepository.get(ticker)
            if (status != null) listOf(status) else emptyList()
        } else {
            investmentStatusRepository.findAll()
        }

        logger.info("[GenerateOrders] 대상 종목: ${targets.size}개")

        // 각 종목 주문 생성
        return targets.mapNotNull { status ->
            try {
                generateSingle(status)
            } catch (e: Exception) {
                logger.error("[GenerateOrders] [${status.ticker}] 주문 생성 실패", e)
                null // 실패한 종목은 제외
            }
        }
    }

    private suspend fun generateSingle(status: InvestmentStatus): OrderResult? {
        val ticker = status.ticker

        val currentStatus = investmentStatusRepository.get(ticker)
        if (currentStatus == null) {
            logger.warn("[GenerateOrders] [$ticker] DB에 상태 정보 없음")
            return null
        }

        val currentPrice = stockRepository.getCurrentPrice(ticker)?.price?.toDoubleOrNull() ?: 0.0
        if (currentPrice == 0.0) {
            logger.warn("[GenerateOrders] [$ticker] 현재가 조회 실패")
            return null
        }

        val holding = accountRepository.getBalance(ticker)
        val currentQuantity = holding?.quantity?.toDoubleOrNull()?.toInt() ?: 0

        // 매도 주문 생성
        val sellOrders = try {
            generateSellOrders(
                status = currentStatus,
                currentQuantity = currentQuantity,
            )
        } catch (e: Exception) {
            logger.error("[GenerateOrders] [$ticker] 매도 주문 생성 실패", e)
            throw e
        }

        // 최저 매도 가격 계산 (MOC 주문 제외)
        val minSellPrice = sellOrders.filter { it.price > 0 }.minOfOrNull { it.price } ?: Double.MAX_VALUE

        // 매수 주문 생성
        val buyOrders = try {
            generateBuyOrders(
                status = currentStatus,
                currentPrice = currentPrice,
                maxBuyPrice = if (minSellPrice < Double.MAX_VALUE) minSellPrice - 0.01 else null
            )
        } catch (e: Exception) {
            logger.error("[GenerateOrders] [$ticker] 매수 주문 생성 실패", e)
            throw e
        }

        logger.info("[GenerateOrders] [$ticker] 주문 생성 완료 - 매수: ${buyOrders.size}개, 매도: ${sellOrders.size}개")

        // 주문 API 전송
        try {
            stockRepository.postOrder(buyOrders, sellOrders)
            logger.info("[GenerateOrders] [$ticker] 주문 전송 완료")
        } catch (e: Exception) {
            logger.error("[GenerateOrders] [$ticker] 주문 전송 실패", e)
            throw e
        }

        return OrderResult(
            ticker = ticker,
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
     *
     * @param maxBuyPrice 최대 매수 가격 (매도 가격보다 낮게 설정)
     */
    private fun generateBuyOrders(
        status: InvestmentStatus,
        currentPrice: Double,
        maxBuyPrice: Double? = null
    ): List<OrderRequest> {
        val orders = mutableListOf<OrderRequest>()

        // 별% LOC 매수 가격
        val rawStarBuyPrice = currentPrice * (1.0 + status.starPercent / 100.0)
        val starBuyPrice = if (maxBuyPrice != null && rawStarBuyPrice >= maxBuyPrice) {
            logger.info("[GenerateOrders] [${status.ticker}] 별% 매수가 조정: ${"%.2f".format(rawStarBuyPrice)} -> ${"%.2f".format(maxBuyPrice)}")
            maxBuyPrice
        } else {
            rawStarBuyPrice
        }

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
                    val avgBuyPrice = if (maxBuyPrice != null && status.avgPrice >= maxBuyPrice) {
                        logger.info("[GenerateOrders] [${status.ticker}] 평단가 매수가 조정: ${"%.2f".format(status.avgPrice)} -> ${"%.2f".format(maxBuyPrice)}")
                        maxBuyPrice
                    } else {
                        status.avgPrice
                    }

                    val avgBuyQty = (halfAmount / avgBuyPrice).toInt()
                    if (avgBuyQty > 0) {
                        orders.add(OrderRequest(
                            ticker = status.ticker,
                            exchange = status.exchange,
                            side = OrderSide.BUY,
                            type = OrderType.LOC,
                            price = avgBuyPrice,
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

        val crashRates = listOf(0.05, 0.10, 0.15)
        crashRates.forEach { rate ->
            val rawCrashPrice = currentPrice * (1.0 - rate)
            val crashPrice = if (maxBuyPrice != null && rawCrashPrice >= maxBuyPrice) {
                logger.info("[GenerateOrders] [${status.ticker}] 폭락대비 매수가(-${(rate * 100).toInt()}%) 조정: ${"%.2f".format(rawCrashPrice)} -> ${"%.2f".format(maxBuyPrice)}")
                maxBuyPrice
            } else {
                rawCrashPrice
            }

            if (crashPrice > 0) {
                orders.add(OrderRequest(
                    ticker = status.ticker,
                    exchange = status.exchange,
                    side = OrderSide.BUY,
                    type = OrderType.LOC,
                    price = crashPrice,
                    quantity = 1
                ))
            }
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
