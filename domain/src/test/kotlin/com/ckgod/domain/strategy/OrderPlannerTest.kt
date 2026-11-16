package com.ckgod.domain.strategy

import com.ckgod.domain.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrderPlannerTest {

    private fun status(
        totalInvested: Double, oneTimeAmount: Double, avgPrice: Double,
        division: Int = 30, mode: StarMode = StarMode.P1_2
    ) = InvestmentStatus(
        ticker = "SOXL", totalInvested = totalInvested, oneTimeAmount = oneTimeAmount, initialCapital = oneTimeAmount * division,
        division = division, avgPrice = avgPrice, targetRate = 20.0, realizedTotalProfit = 0.0, updatedAt = "", starMode = mode
    )

    @Test
    fun `후반전은 1회 매수금 전부를 별% LOC 로 사고 매도가보다 1센트 낮춘다`() {
        // 2026-11-16 SOXL 재설정 직후: T 27.57, 별% -16.76
        val plan = OrderPlanner.plan(status(10748.83, 390.0, 85.9906), basePrice = 58.3, livePrice = 58.3, currentQuantity = 125)

        val buy = plan.buyOrders.single()
        assertEquals(OrderType.LOC, buy.type)
        assertEquals(71.57, buy.price)
        assertEquals(5, buy.quantity)
        assertEquals(listOf(31 to OrderType.LOC, 94 to OrderType.LIMIT), plan.sellOrders.map { it.quantity to it.type })
        assertEquals(listOf(71.58, 103.19), plan.sellOrders.map { it.price })
    }

    @Test
    fun `별% 1주가 1회 매수금 절반보다 비싸면 1주를 강제로 산다`() {
        val plan = OrderPlanner.plan(status(1000.0, 100.0, 80.0, division = 40), basePrice = 75.0, livePrice = 75.0, currentQuantity = 12)

        val star = plan.buyOrders.first()
        assertEquals(1, star.quantity)
        assertTrue(plan.notes.any { it.contains("1주 강제 매수") })
    }

    @Test
    fun `쿼터모드는 4분의 1 을 MOC 로 팔고 매수하지 않는다`() {
        val plan = OrderPlanner.plan(status(29.5 * 390.0, 390.0, 80.0), basePrice = 60.0, livePrice = 60.0, currentQuantity = 100)

        assertEquals(emptyList(), plan.buyOrders)
        assertEquals(OrderType.MOC, plan.sellOrders.first().type)
        assertEquals(25, plan.sellOrders.first().quantity)
    }

    @Test
    fun `보유가 없으면 매도 없이 첫 진입 매수만 한다`() {
        val plan = OrderPlanner.plan(status(0.0, 400.0, 0.0), basePrice = 50.0, livePrice = 50.0, currentQuantity = 0)

        assertEquals(emptyList(), plan.sellOrders)
        assertEquals(1, plan.buyOrders.count { it.crashRate == null })
        assertEquals(listOf(0.07, 0.09, 0.12, 0.15), plan.buyOrders.mapNotNull { it.crashRate })
    }
}
