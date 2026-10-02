package com.ckgod.domain.usecase

import com.ckgod.domain.backtest.BacktestTradeHistoryRepository
import com.ckgod.domain.model.OpenOrder
import com.ckgod.domain.model.OrderSide
import com.ckgod.domain.model.OrderStatus
import com.ckgod.domain.model.OrderType
import com.ckgod.domain.model.TradeHistory
import com.ckgod.domain.repository.OrderManagementRepository
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ManageOrdersUseCaseTest {

    private class FakeOrderRepository(
        var openOrders: List<OpenOrder>,
        var failWith: Exception? = null
    ) : OrderManagementRepository {
        val canceled = mutableListOf<String>()
        val modified = mutableListOf<Triple<String, Int, Double>>()

        override suspend fun getOpenOrders() = openOrders
        override suspend fun modify(order: OpenOrder, quantity: Int, price: Double): String {
            failWith?.let { throw it }
            modified += Triple(order.orderNo, quantity, price)
            return "NEW-${order.orderNo}"
        }
        override suspend fun cancel(order: OpenOrder) {
            failWith?.let { throw it }
            canceled += order.orderNo
        }
    }

    private val open = OpenOrder(
        orderNo = "0001", originalOrderNo = null, ticker = "SOXL", exchange = "AMEX",
        side = OrderSide.BUY, orderPrice = 20.0, orderQuantity = 5, filledQuantity = 1,
        unfilledQuantity = 4, orderDate = "20261002", orderTime = "180512"
    )

    private fun history(orderNo: String) = TradeHistory(
        ticker = "SOXL", orderNo = orderNo, orderSide = OrderSide.BUY, orderType = OrderType.LIMIT,
        orderPrice = 20.0, orderQuantity = 5, orderTime = LocalDateTime.now(), tValue = 3.5
    )

    @Test
    fun `미체결 목록에 없는 주문은 취소하지 않는다`() = runBlocking {
        val repo = FakeOrderRepository(listOf(open))
        val result = ManageOrdersUseCase(repo, BacktestTradeHistoryRepository()).cancel("9999")

        assertFalse(result.success)
        assertTrue(repo.canceled.isEmpty())
    }

    @Test
    fun `빈 주문번호는 거부 기록과 섞이지 않도록 바로 실패한다`() = runBlocking {
        val repo = FakeOrderRepository(listOf(open.copy(orderNo = "")))
        val result = ManageOrdersUseCase(repo, BacktestTradeHistoryRepository()).cancel("")

        assertFalse(result.success)
        assertTrue(repo.canceled.isEmpty())
    }

    @Test
    fun `취소하면 기록 상태가 CANCELED 가 된다`() = runBlocking {
        val repo = FakeOrderRepository(listOf(open))
        val histories = BacktestTradeHistoryRepository().apply { save(history("0001")) }

        val result = ManageOrdersUseCase(repo, histories).cancel("0001")

        assertTrue(result.success)
        assertEquals(listOf("0001"), repo.canceled)
        assertEquals(OrderStatus.CANCELED, histories.findByOrderNo("0001")?.status)
    }

    @Test
    fun `정정 수량이 미체결 수량을 넘으면 보내지 않는다`() = runBlocking {
        val repo = FakeOrderRepository(listOf(open))
        val result = ManageOrdersUseCase(repo, BacktestTradeHistoryRepository()).modify("0001", 19.5, 5)

        assertFalse(result.success)
        assertTrue(repo.modified.isEmpty())
    }

    @Test
    fun `정정하면 원주문은 CANCELED, 새 주문번호로 PENDING 기록이 생긴다`() = runBlocking {
        val repo = FakeOrderRepository(listOf(open))
        val histories = BacktestTradeHistoryRepository().apply { save(history("0001")) }

        val result = ManageOrdersUseCase(repo, histories).modify("0001", 19.5, null)

        assertTrue(result.success)
        assertEquals("NEW-0001", result.newOrderNo)
        assertEquals(Triple("0001", 4, 19.5), repo.modified.single())  // 수량 생략 시 미체결 4주 전체
        assertEquals(OrderStatus.CANCELED, histories.findByOrderNo("0001")?.status)
        val renewed = histories.findByOrderNo("NEW-0001")
        assertEquals(OrderStatus.PENDING, renewed?.status)
        assertEquals(19.5, renewed?.orderPrice)
        assertEquals(4, renewed?.orderQuantity)
    }

    @Test
    fun `KIS 가 거부하면 실패를 돌려주고 기록은 그대로 둔다`() = runBlocking {
        val repo = FakeOrderRepository(listOf(open), failWith = RuntimeException("[APBK0001] 정정 불가"))
        val histories = BacktestTradeHistoryRepository().apply { save(history("0001")) }

        val result = ManageOrdersUseCase(repo, histories).cancel("0001")

        assertFalse(result.success)
        assertTrue(result.message.contains("정정 불가"))
        assertEquals(OrderStatus.PENDING, histories.findByOrderNo("0001")?.status)
        assertNull(histories.findByOrderNo("NEW-0001"))
    }
}
