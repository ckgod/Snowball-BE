package com.ckgod.domain.backtest

import com.ckgod.domain.model.OrderSide
import com.ckgod.domain.model.OrderStatus
import com.ckgod.domain.model.OrderType
import com.ckgod.domain.model.TradeHistory
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BacktestTradeHistoryRepositoryTest {

    private fun history(orderNo: String) = TradeHistory(
        ticker = "SOXL", orderNo = orderNo, orderSide = OrderSide.BUY, orderType = OrderType.LOC,
        orderPrice = 20.0, orderQuantity = 5, orderTime = LocalDateTime.now(), tValue = 3.5
    )

    @Test
    fun `미체결 목록은 상태 변경을 따라가고 저장 순서를 지킨다`() = runBlocking {
        val repo = BacktestTradeHistoryRepository()
        listOf("BT-1", "BT-2", "BT-3", "BT-4").forEach { repo.save(history(it)) }

        repo.updateOrderStatus("BT-2", OrderStatus.FILLED, 5, 19.5, LocalDateTime.now(), null)
        repo.markRejected("BT-3", "거부")
        repo.updateStatus("BT-4", OrderStatus.CANCELED)
        repo.updateStatus("BT-4", OrderStatus.PENDING)

        assertEquals(listOf("BT-1", "BT-4"), repo.findPendingOrders().map { it.orderNo })
        assertEquals(OrderStatus.FILLED, repo.findByOrderNo("BT-2")?.status)
        assertEquals(19.5, repo.findByOrderNo("BT-2")?.filledPrice)
        assertEquals("거부", repo.findByOrderNo("BT-3")?.failReason)
    }

    @Test
    fun `없는 주문번호는 무시한다`() = runBlocking {
        val repo = BacktestTradeHistoryRepository()
        repo.save(history("BT-1"))

        repo.updateStatus("BT-9", OrderStatus.FILLED)

        assertNull(repo.findByOrderNo("BT-9"))
        assertEquals(1, repo.findPendingOrders().size)
    }
}
