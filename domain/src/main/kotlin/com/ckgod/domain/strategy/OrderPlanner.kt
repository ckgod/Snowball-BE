package com.ckgod.domain.strategy

import com.ckgod.domain.model.InvestmentStatus
import com.ckgod.domain.model.OrderRequest
import com.ckgod.domain.model.OrderSide
import com.ckgod.domain.model.OrderType
import com.ckgod.domain.model.TradePhase
import com.ckgod.domain.utils.roundTo2Decimal
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

/**
 * 무한매수법 하루치 주문 계산. 저장소·API 를 부르지 않고 상태와 가격만으로 주문 목록을 만든다.
 * 실주문(PlaceStrategyOrdersUseCase)과 백테스트가 같은 계산을 쓴다.
 */
object OrderPlanner {

    /**
     * @param notes 계산 중 기본 규칙에서 벗어난 조정(1주 강제 매수, LOC 상한 등). 실주문은 로그로 남긴다
     */
    data class Plan(
        val buyOrders: List<OrderRequest>,
        val sellOrders: List<OrderRequest>,
        val notes: List<String>
    )

    /**
     * @param basePrice 기준가 = 전일 정규장 종가
     * @param livePrice 현재 호가. LOC 매수가 상한 계산에만 쓴다
     * @param currentQuantity 계좌의 실제 보유 수량
     */
    fun plan(status: InvestmentStatus, basePrice: Double, livePrice: Double?, currentQuantity: Int): Plan {
        val notes = mutableListOf<String>()
        val sellOrders = generateSellOrders(status, currentQuantity)
        // 매수가는 가장 낮은 매도가보다 1센트 아래로 둔다 (MOC 는 가격이 없어 제외)
        val minSellPrice = sellOrders.filter { it.price > 0 }.minOfOrNull { it.price }
        val locBuyCap = locBuyPriceCap(basePrice, livePrice)
        val buyOrders = generateBuyOrders(
            status = status,
            basePrice = basePrice,
            maxBuyPrice = minSellPrice?.let { it - 0.01 },
            notes = notes
        ).map { capLocBuyPrice(it, locBuyCap, notes) }
        return Plan(buyOrders, sellOrders, notes)
    }

    /**
     * LOC 매수가 상한.
     *
     * 미국 거래소는 호가보다 일정 비율(SOXL 기준 30%) 넘게 높은 매수 지정가를 접수 직후 거부한다.
     * 주가가 평단·별% 기준가보다 크게 떨어지면 별%·평단 LOC 매수가가 이 한도를 넘어
     * 매일 가장 큰 매수가 빠진다.
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

    private fun capLocBuyPrice(order: OrderRequest, cap: Double, notes: MutableList<String>): OrderRequest {
        if (order.side != OrderSide.BUY || order.type != OrderType.LOC || order.price <= cap) return order
        notes += "LOC 매수가 상한 적용: ${"%.2f".format(order.price)} -> ${"%.2f".format(cap)}"
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
        maxBuyPrice: Double?,
        notes: MutableList<String>
    ): List<OrderRequest> {
        val orders = mutableListOf<OrderRequest>()

        val rawStarBuyPrice = status.getBuyPrice(basePrice)

        val starBuyPrice = if (maxBuyPrice != null && rawStarBuyPrice >= maxBuyPrice) {
            maxBuyPrice.roundTo2Decimal()
        } else {
            rawStarBuyPrice
        }

        when (status.phase) {
            TradePhase.FRONT_HALF -> {
                if (status.avgPrice > 0) {

                    // 별% 수량: KDoc 의 Case A/B/C
                    val halfAmount = status.oneTimeAmount / 2.0
                    val starBuyQty = when {
                        starBuyPrice <= halfAmount -> (halfAmount / starBuyPrice).toInt()
                        starBuyPrice <= status.oneTimeAmount -> 1
                        else -> 0
                    }
                    val usedAmountForStar = starBuyQty * starBuyPrice

                    if (starBuyQty > 0) {
                        if (starBuyPrice > halfAmount) {
                            notes += "별% 1주 단가(${"%.2f".format(starBuyPrice)})가 halfAmount(${"%.2f".format(halfAmount)}) 초과 - 매수 누락 방지를 위해 1주 강제 매수"
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
                        notes += "별% 1주 단가(${"%.2f".format(starBuyPrice)})가 1회 매수금(${"%.2f".format(status.oneTimeAmount)}) 초과 - 별% 매수 포기, 평단가에 전액 할당"
                    }

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
                    notes += "폭락대비 매수가(-${(rate * 100).toInt()}%) 조정: ${"%.2f".format(rawCrashPrice)} -> ${"%.2f".format(maxBuyPrice)}"
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
     * 전반전·후반전 (T < division - 1)
     * - 보유 수량의 1/4 을 별% LOC 매도
     * - 나머지 3/4 를 목표가(targetRate) 지정가 매도
     *
     * 쿼터모드 (division - 1 <= T < division)
     * - 보유 수량의 1/4 을 MOC 매도 (쿼터 손절)
     * - 나머지 3/4 를 목표가(targetRate) 지정가 매도
     */
    private fun generateSellOrders(
        status: InvestmentStatus,
        currentQuantity: Int,
    ): List<OrderRequest> {
        if (currentQuantity == 0 || status.avgPrice == 0.0) {
            return emptyList()
        }

        val orders = mutableListOf<OrderRequest>()

        val quarterQty = (currentQuantity / 4.0).toInt()
        val threeQuarterQty = currentQuantity - quarterQty

        when(status.phase) {
            TradePhase.FRONT_HALF, TradePhase.BACK_HALF -> {
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
                if (quarterQty > 0) {
                    orders.add(OrderRequest(
                        ticker = status.ticker,
                        exchange = status.exchange,
                        side = OrderSide.SELL,
                        type = OrderType.MOC,
                        price = 0.0,
                        quantity = quarterQty
                    ))
                }

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

    /** 거래소 한도(30%)보다 여유를 둔다. 프리장 호가가 주문 사이에 움직여도 걸리지 않게 */
    private val LOC_BUY_CAP_RATE: BigDecimal = BigDecimal("0.25")
}
