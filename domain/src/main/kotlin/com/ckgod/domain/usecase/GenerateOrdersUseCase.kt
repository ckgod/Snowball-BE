package com.ckgod.domain.usecase

import com.ckgod.domain.model.*
import com.ckgod.domain.repository.AccountRepository
import com.ckgod.domain.repository.InvestmentStatusRepository
import com.ckgod.domain.repository.StockRepository
import com.ckgod.domain.repository.TradeHistoryRepository
import com.ckgod.domain.utils.roundTo2Decimal
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode
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
    private val tradeHistoryRepository: TradeHistoryRepository,
    /** 실주문 직후 거래소 거부 확인. 백테스트는 null (확인 안 함) */
    private val rejectionChecker: OrderRejectionChecker? = null,
    /** 백테스트는 하루마다 부르므로 NOP 로거를 넘긴다 (10년치면 로그 수만 줄) */
    private val logger: Logger = LoggerFactory.getLogger(GenerateOrdersUseCase::class.java)
) {

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
        val marketPrice = stockRepository.getCurrentPrice(ticker)
        val basePrice = marketPrice?.previousClose?.toDoubleOrNull() ?: 0.0
        if (basePrice == 0.0) {
            logger.warn("[GenerateOrders] [$ticker] 기준가(전일 종가) 조회 실패")
            return null
        }
        val locBuyCap = locBuyPriceCap(basePrice, marketPrice?.price?.toDoubleOrNull())

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
            ).map { capLocBuyPrice(it, locBuyCap) }
        } catch (e: Exception) {
            logger.error("[GenerateOrders] [$ticker] 매수 주문 생성 실패", e)
            throw e
        }

        logger.info("[GenerateOrders] [$ticker] 주문 생성 완료 - 매수: ${buyOrders.size}개, 매도: ${sellOrders.size}개")

        // 주문 API 전송
        val submission = try {
            stockRepository.postOrder(buyOrders, sellOrders)
        } catch (e: Exception) {
            logger.error("[GenerateOrders] [$ticker] 주문 전송 실패", e)
            throw e
        }
        logger.info("[GenerateOrders] [$ticker] 주문 전송 완료 - 성공: ${submission.accepted.size}개, 실패: ${submission.rejected.size}개")

        // 주문 내역 DB 저장
        submission.accepted.forEach { response ->
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

        // 실패한 주문도 기록해 앱에서 확인할 수 있게 한다. KIS 주문번호가 없으므로 orderNo 는 빈 문자열.
        // 체결 동기화는 KIS 체결 내역의 주문번호로만 갱신하므로 이 행은 건드리지 않는다.
        submission.rejected.forEach { rejection ->
            val history = TradeHistory(
                ticker = ticker,
                orderNo = "",
                orderSide = rejection.request.side,
                orderType = rejection.request.type,
                orderPrice = rejection.request.price,
                orderQuantity = rejection.request.quantity,
                orderTime = LocalDateTime.now(ZoneId.of("Asia/Seoul")),
                status = OrderStatus.REJECTED,
                tValue = currentStatus.tValue,
                crashRate = rejection.request.crashRate,
                failReason = rejection.reason
            )
            tradeHistoryRepository.save(history)
        }
        logger.info("[GenerateOrders] [$ticker] 주문 내역 DB 저장 완료")

        // 주문번호를 받은 뒤 거래소가 거부한 주문을 바로 REJECTED 로 고친다 (안 그러면 새벽 정산 전까지 PENDING 으로 보인다)
        rejectionChecker?.findRejected(submission.accepted.map { it.orderNo })?.forEach { (orderNo, reason) ->
            logger.warn("[GenerateOrders] [$ticker] 거래소 거부 - $orderNo: $reason")
            // 주문은 이미 나갔으니 기록 실패로 Job 을 실패시키지 않는다. 새벽 정산이 다시 고친다
            runCatching { tradeHistoryRepository.markRejected(orderNo, reason) }
                .onFailure { logger.error("[GenerateOrders] [$ticker] 거부 기록 실패 - $orderNo", it) }
        }

        return OrderResult(
            ticker = ticker,
            currentPrice = basePrice,
            buyOrders = buyOrders,
            sellOrders = sellOrders
        )
    }

    /**
     * LOC 매수가 상한.
     *
     * 미국 거래소는 호가보다 일정 비율(SOXL 기준 30%) 넘게 높은 매수 지정가를 접수 직후 거부한다.
     * 주가가 평단·별% 기준가보다 크게 떨어지면 별%·평단 LOC 매수가가 이 한도를 넘어
     * 매일 가장 큰 매수가 빠진다 (2026-10-13~14 SOXL 166.05·164.05 거부, 호가 약 126).
     * LOC 는 종가에 체결되므로 상한으로 낮춰도 장중 상한 넘게 오르지 않는 한 결과가 같다.
     *
     * 기준은 전일 종가와 현재 호가 중 낮은 쪽이다 (프리장 하락 시 호가 기준으로 잘린다).
     */
    private fun locBuyPriceCap(basePrice: Double, livePrice: Double?): Double {
        val reference = livePrice?.takeIf { it > 0.0 }?.let { minOf(it, basePrice) } ?: basePrice
        // Double 곱셈 오차로 한 틱 내려가지 않게 (125.96 * 1.25 = 157.4499..) 십진수로 계산한다
        return BigDecimal.valueOf(reference)
            .multiply(BigDecimal.ONE + LOC_BUY_CAP_RATE)
            .setScale(2, RoundingMode.FLOOR)
            .toDouble()
    }

    private fun capLocBuyPrice(order: OrderRequest, cap: Double): OrderRequest {
        if (order.side != OrderSide.BUY || order.type != OrderType.LOC || order.price <= cap) return order
        logger.info("[GenerateOrders] [${order.ticker}] LOC 매수가 상한 적용: ${"%.2f".format(order.price)} -> ${"%.2f".format(cap)}")
        return order.copy(price = cap)
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

    private companion object {
        /** 거래소 한도(30%)보다 여유를 둔다. 프리장 호가가 주문 사이에 움직여도 걸리지 않게 */
        val LOC_BUY_CAP_RATE: BigDecimal = BigDecimal("0.25")
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
