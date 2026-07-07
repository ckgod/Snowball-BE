package com.ckgod.domain.usecase

import com.ckgod.domain.model.*
import com.ckgod.domain.repository.AccountRepository
import com.ckgod.domain.repository.InvestmentStatusRepository
import com.ckgod.domain.repository.StockRepository
import com.ckgod.domain.repository.TradeHistoryRepository
import com.ckgod.domain.utils.roundTo2Decimal
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs

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
    private val investmentStatusRepository: InvestmentStatusRepository,
    private val tradeHistoryRepository: TradeHistoryRepository
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

        // 주문 기준가는 직전 정규장 종가(previousClose)를 사용한다.
        // OrderJob(18:00 KST) 시점은 미국 프리마켓 진행 중이라 last(현재가)에 프리장 시세가 반환되는데,
        // 무한매수법의 폭락 대비 LOC·첫 진입 별% 기준은 전일 정규장 종가여야 한다.
        // (2026-06-09 SOXL: 프리장 +6.7% 갭업가가 기준이 되어 ladder 3건이 초과 체결된 사례)
        val basePrice = stockRepository.getCurrentPrice(ticker)?.previousClose?.toDoubleOrNull() ?: 0.0
        if (basePrice == 0.0) {
            logger.warn("[GenerateOrders] [$ticker] 기준가(전일 종가) 조회 실패")
            return null
        }

        val holding = accountRepository.getBalance(ticker)
        val currentQuantity = holding?.quantity?.toDoubleOrNull()?.toInt() ?: 0
        val currentAvgPrice = holding?.avgPrice?.toDoubleOrNull() ?: 0.0

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
                basePrice = basePrice,
                maxBuyPrice = if (minSellPrice < Double.MAX_VALUE) minSellPrice - 0.01 else null
            )
        } catch (e: Exception) {
            logger.error("[GenerateOrders] [$ticker] 매수 주문 생성 실패", e)
            throw e
        }

        logger.info("[GenerateOrders] [$ticker] 주문 생성 완료 - 매수: ${buyOrders.size}개, 매도: ${sellOrders.size}개")

        // 주문 API 전송
        val orderResponses = try {
            stockRepository.postOrder(buyOrders, sellOrders)
        } catch (e: Exception) {
            logger.error("[GenerateOrders] [$ticker] 주문 전송 실패", e)
            throw e
        }
        logger.info("[GenerateOrders] [$ticker] 주문 전송 완료 - 성공: ${orderResponses.size}개")

        // 주문 내역 DB 저장
        orderResponses.forEach { response ->
            val orderDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))

            val history = TradeHistory(
                ticker = ticker,
                orderNo = response.orderNo,
                orderSide = response.request.side,
                orderType = response.request.type,
                orderPrice = response.request.price,
                orderQuantity = response.request.quantity,
                orderTime = orderDateTime,
                status = OrderStatus.PENDING,
                tValue = currentStatus.tValue,
                crashRate = response.request.crashRate,
                avgPrice = if (response.request.side == OrderSide.SELL) currentAvgPrice else 0.0 // 매도 주문의 경우만 평단가 입력
            )
            tradeHistoryRepository.save(history)
        }
        logger.info("[GenerateOrders] [$ticker] 주문 내역 DB 저장 완료")

        return OrderResult(
            ticker = ticker,
            currentPrice = basePrice,
            buyOrders = buyOrders,
            sellOrders = sellOrders
        )
    }

    /**
     * 매수 주문 생성
     *
     * 현재 T값에 따라 매수 분배가 다름
     *
     * 1. 전반전 (StarMode에 따라 T <= division/2 또는 T <= division*2/3)
     *    표준: 1회 매수액의 절반(halfAmount)은 별% LOC, 나머지 절반은 평단가(0%) LOC
     *
     *    [고가 종목 보완 — SOXL/TQQQ/FNGU 등 1주 단가가 halfAmount를 초과하는 케이스]
     *    표준 로직대로 `starBuyQty = (halfAmount / starBuyPrice).toInt()` 만 쓰면
     *    별% 매수가 1주가 halfAmount보다 비쌀 때 starBuyQty=0이 되어 별% LOC 주문이 누락된다.
     *    매수 누락은 T값 정체 → starPercent 회복 지연 → 사이클 종료 불가의 악순환을 만들어
     *    평가손실을 시간에 비례해 고착화시키므로, 1회 매수금 한도 내에서 다음 우선순위로 보완한다.
     *    - Case A: 별% 1주 ≤ halfAmount       → halfAmount / starBuyPrice (표준)
     *    - Case B: halfAmount < 별% 1주 ≤ oneTimeAmount → 별% 1주 강제 매수 (누락 방지)
     *    - Case C: 별% 1주 > oneTimeAmount    → 별% 포기, 1회 매수금 전체를 평단가에 할당
     *    어떤 경우든 평단가 LOC는 (1회 매수금 - 별% 사용액)을 남김없이 소진해 매수 기회를 최대화한다.
     *
     * 2. 후반전 (전반전 종료 후 ~ T < division - 1)
     *    1회 매수액 전체를 별% LOC로 매수 시도
     *
     * 전후반 공통으로 크게 하락하는 경우를 대비해, 1회 정액 매수를 맞추기 위해 아래로 LOC 매수를 추가 시도한다.
     *
     * @param basePrice 기준가 = 직전 정규장 종가. 폭락 대비 LOC와 첫 진입 별% 계산의 기준.
     * @param maxBuyPrice 최대 매수 가격 (매도 가격보다 낮게 설정)
     */
    private fun generateBuyOrders(
        status: InvestmentStatus,
        basePrice: Double,
        maxBuyPrice: Double? = null
    ): List<OrderRequest> {
        val orders = mutableListOf<OrderRequest>()

        // 별% LOC 매수 가격
        val rawStarBuyPrice = status.getBuyPrice(basePrice)

        val starBuyPrice = if (maxBuyPrice != null && rawStarBuyPrice >= maxBuyPrice) {
            maxBuyPrice.roundTo2Decimal()
        } else {
            rawStarBuyPrice
        }

        when (status.phase) {
            TradePhase.FRONT_HALF -> {
                // 첫 진입이 아닐 경우 기본 로직
                if (status.avgPrice > 0) {

                    // 1. 별% LOC 매수
                    //
                    // halfAmount(1회 매수금의 절반)을 기준으로 별% 수량을 결정하되,
                    // 고가 종목에서 1주조차 못 사 매수가 누락되는 사태를 피하기 위해
                    // 3단계 우선순위로 fallback 한다. (함수 KDoc의 Case A/B/C 참조)
                    //   - Case A: 별% 1주 ≤ halfAmount       → 표준: halfAmount / starBuyPrice
                    //   - Case B: halfAmount < 별% 1주 ≤ oneTimeAmount → 별% 1주 강제
                    //   - Case C: 별% 1주 > oneTimeAmount    → 별% 포기 (평단가에 전액)
                    val halfAmount = status.oneTimeAmount / 2.0
                    val starBuyQty = when {
                        starBuyPrice <= halfAmount -> (halfAmount / starBuyPrice).toInt()
                        starBuyPrice <= status.oneTimeAmount -> 1
                        else -> 0
                    }
                    val usedAmountForStar = starBuyQty * starBuyPrice

                    if (starBuyQty > 0) {
                        if (starBuyPrice > halfAmount) {
                            logger.info("[GenerateOrders] [${status.ticker}] 별% 1주 단가(${"%.2f".format(starBuyPrice)})가 halfAmount(${"%.2f".format(halfAmount)}) 초과 - 매수 누락 방지를 위해 1주 강제 매수")
                        }
                        orders.add(OrderRequest(
                            ticker = status.ticker,
                            exchange = status.exchange,
                            side = OrderSide.BUY,
                            type = OrderType.LOC,
                            price = starBuyPrice,
                            quantity = starBuyQty,
                        ))
                    } else {
                        logger.info("[GenerateOrders] [${status.ticker}] 별% 1주 단가(${"%.2f".format(starBuyPrice)})가 1회 매수금(${"%.2f".format(status.oneTimeAmount)}) 초과 - 별% 매수 포기, 평단가에 전액 할당")
                    }

                    // 2. 평단가(0%) LOC 매수
                    //
                    // 1회 매수금에서 별% 사용액을 뺀 금액을 평단가 LOC에 모두 할당해
                    // 매수 기회를 최대화한다. Case C에서는 1회 매수금 전체가 평단가로 들어간다.
                    val remainAmount = status.oneTimeAmount - usedAmountForStar

                    val avgBuyPrice = if (maxBuyPrice != null && status.avgPrice >= maxBuyPrice) {
                        maxBuyPrice
                    } else {
                        status.avgPrice
                    }.roundTo2Decimal()

                    val avgBuyQty = (remainAmount / avgBuyPrice).toInt()

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
                // 첫 진입일 경우 1회 매수액 전부 소비
                else {
                    val starBuyQty = (status.oneTimeAmount / starBuyPrice).toInt()
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

        if (status.phase != TradePhase.QUARTER_MODE && status.oneTimeAmount > 0) {
            val crashRates = listOf(0.07, 0.09, 0.12, 0.15)
            crashRates.forEach { rate ->
                if (status.starPercent < 0 && (rate * 100).toInt() < abs(status.starPercent.toInt())) {
                    return@forEach
                }

                val rawCrashPrice = basePrice * (1.0 - rate)
                val crashPrice = if (maxBuyPrice != null && rawCrashPrice >= maxBuyPrice) {
                    logger.info("[GenerateOrders] [${status.ticker}] 폭락대비 매수가(-${(rate * 100).toInt()}%) 조정: ${"%.2f".format(rawCrashPrice)} -> ${"%.2f".format(maxBuyPrice)}")
                    maxBuyPrice
                } else {
                    rawCrashPrice
                }.roundTo2Decimal()

                if (crashPrice > 0) {
                    orders.add(OrderRequest(
                        ticker = status.ticker,
                        exchange = status.exchange,
                        side = OrderSide.BUY,
                        type = OrderType.LOC,
                        price = crashPrice,
                        quantity = 1,
                        crashRate = rate
                    ))
                }
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
                        price = status.starSellPrice,
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
                        price = status.targetSellPrice,
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
                        price = status.targetSellPrice,
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
