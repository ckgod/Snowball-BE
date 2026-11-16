package com.ckgod.domain.model

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class TradeHistoryTest {

    private val sell = OrderRequest("SOXL", Exchange.AMEX, OrderSide.SELL, OrderType.LIMIT, 103.19, 94)
    private val now = LocalDateTime.of(2026, 11, 16, 18, 5)

    @Test
    fun `접수된 매도는 PENDING 과 평단으로 기록한다`() {
        val h = TradeHistory.of(OrderResponse(sell, "0001", "180500"), tValue = 27.57, sellAvgPrice = 85.99, orderTime = now)

        assertEquals("0001", h.orderNo)
        assertEquals(OrderStatus.PENDING, h.status)
        assertEquals(85.99, h.avgPrice)
    }

    @Test
    fun `접수 실패는 주문번호 없이 REJECTED 와 사유로 기록한다`() {
        val h = TradeHistory.of(OrderRejection(sell, "[APBK0952] 주문가능금액 초과"), tValue = 27.57, sellAvgPrice = 85.99, orderTime = now)

        assertEquals("", h.orderNo)
        assertEquals(OrderStatus.REJECTED, h.status)
        assertEquals("[APBK0952] 주문가능금액 초과", h.failReason)
    }

    @Test
    fun `주문번호를 받은 뒤 거래소가 거부하면 주문번호와 함께 REJECTED 다`() {
        val h = TradeHistory.of(OrderResponse(sell, "0002", "180500"), 27.57, 85.99, now, isManual = true, exchangeRejectReason = "Limit Price Too Aggressive")

        assertEquals("0002", h.orderNo)
        assertEquals(OrderStatus.REJECTED, h.status)
        assertEquals(true, h.isManual)
    }

    @Test
    fun `매수는 평단을 남기지 않는다`() {
        val buy = sell.copy(side = OrderSide.BUY, type = OrderType.LOC)
        assertEquals(0.0, TradeHistory.of(OrderResponse(buy, "3", "t"), 1.0, 85.99, now).avgPrice)
    }
}
