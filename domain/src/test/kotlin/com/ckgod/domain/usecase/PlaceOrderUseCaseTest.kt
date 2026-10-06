package com.ckgod.domain.usecase

import com.ckgod.domain.backtest.BacktestInvestmentStatusRepository
import com.ckgod.domain.backtest.BacktestTradeHistoryRepository
import com.ckgod.domain.model.*
import com.ckgod.domain.repository.AccountRepository
import com.ckgod.domain.repository.StockRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaceOrderUseCaseTest {

    private class FakeStockRepository(private val reject: String? = null) : StockRepository {
        val sent = mutableListOf<OrderRequest>()
        override suspend fun getCurrentPrice(stockCode: String, includeDayMarket: Boolean): MarketPrice? = null
        override suspend fun getExchangeRate() = 1450.0
        override suspend fun postOrder(buyOrders: List<OrderRequest>, sellOrders: List<OrderRequest>): OrderSubmission {
            val order = (buyOrders + sellOrders).single()
            sent += order
            return if (reject == null) {
                OrderSubmission(listOf(OrderResponse(order, "0001", "180500")), emptyList())
            } else {
                OrderSubmission(emptyList(), listOf(OrderRejection(order, reject)))
            }
        }
    }

    private val account = object : AccountRepository {
        override suspend fun getAccountBalance(): AccountStatus = throw UnsupportedOperationException()
        override suspend fun getBalance(ticker: String): HoldingStock? = null
        override suspend fun getPresentAccountBalance(): PresentAccountStatus = throw UnsupportedOperationException()
        override suspend fun getDailyProfit(ticker: String): List<Double> = emptyList()
        override suspend fun getTotalAsset(): TotalAsset = throw UnsupportedOperationException()
    }

    private fun useCase(stock: FakeStockRepository, histories: BacktestTradeHistoryRepository) =
        PlaceOrderUseCase(stock, account, BacktestInvestmentStatusRepository(), histories)

    private fun command(side: OrderSide = OrderSide.BUY, type: OrderType = OrderType.LIMIT, price: Double = 20.0, quantity: Int = 1) =
        PlaceOrderUseCase.Command(ticker = "soxl", side = side, type = type, price = price, quantity = quantity)

    @Test
    fun `접수되면 수동 PENDING 으로 기록한다`() = runBlocking {
        val stock = FakeStockRepository()
        val histories = BacktestTradeHistoryRepository()

        val result = useCase(stock, histories)(command())

        assertTrue(result.success)
        assertEquals("0001", result.orderNo)
        assertEquals("SOXL", stock.sent.single().ticker)          // 대문자로 정규화
        assertEquals(Exchange.AMEX, stock.sent.single().exchange)  // 종목으로 거래소 추정
        val saved = histories.findByOrderNo("0001")!!
        assertTrue(saved.isManual)
        assertEquals(OrderStatus.PENDING, saved.status)
    }

    @Test
    fun `KIS 거부면 REJECTED 와 사유로 기록한다`() = runBlocking {
        val histories = BacktestTradeHistoryRepository()
        val result = useCase(FakeStockRepository(reject = "[APBK0952] 주문가능금액 초과"), histories)(command())

        assertFalse(result.success)
        val saved = histories.getAllHistories().single()
        assertEquals(OrderStatus.REJECTED, saved.status)
        assertEquals("[APBK0952] 주문가능금액 초과", saved.failReason)
        assertTrue(saved.isManual)
    }

    @Test
    fun `매수에 시장가 계열은 보내지 않는다`() = runBlocking {
        val stock = FakeStockRepository()
        val result = useCase(stock, BacktestTradeHistoryRepository())(command(type = OrderType.MOC, price = 0.0))

        assertFalse(result.success)
        assertTrue(stock.sent.isEmpty())
    }

    @Test
    fun `매도 MOC 는 가격 0 으로 보낸다`() = runBlocking {
        val stock = FakeStockRepository()
        val result = useCase(stock, BacktestTradeHistoryRepository())(command(side = OrderSide.SELL, type = OrderType.MOC, price = 99.0))

        assertTrue(result.success)
        assertEquals(0.0, stock.sent.single().price)
    }

    @Test
    fun `지정가인데 가격이 0 이면 보내지 않는다`() = runBlocking {
        val stock = FakeStockRepository()
        val result = useCase(stock, BacktestTradeHistoryRepository())(command(price = 0.0))
        assertFalse(result.success)
        assertTrue(stock.sent.isEmpty())
    }
}
