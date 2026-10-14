package com.ckgod.domain.usecase

import com.ckgod.domain.backtest.BacktestInvestmentStatusRepository
import com.ckgod.domain.backtest.BacktestTradeHistoryRepository
import com.ckgod.domain.model.*
import com.ckgod.domain.repository.AccountRepository
import com.ckgod.domain.repository.StockRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GenerateOrdersUseCaseTest {

    /** 2026-10-14 SOXL 상황: 평단 146.08, 전일 종가 127.57, 프리장 호가 약 125.96 */
    private class FakeStockRepository(private val previousClose: Double, private val live: Double) : StockRepository {
        val sent = mutableListOf<OrderRequest>()
        override suspend fun getCurrentPrice(stockCode: String, includeDayMarket: Boolean) = MarketPrice(
            ticker = stockCode, price = live.toString(), previousClose = previousClose.toString(), changeRate = "0",
            open = "0", high = "0", low = "0", volume = "0", krwPrice = "0", krwChangeAmount = "0", exchangeRate = "1450"
        )
        override suspend fun getExchangeRate() = 1450.0
        override suspend fun postOrder(buyOrders: List<OrderRequest>, sellOrders: List<OrderRequest>): OrderSubmission {
            val all = buyOrders + sellOrders
            sent += all
            return OrderSubmission(all.mapIndexed { i, o -> OrderResponse(o, "%04d".format(i + 1), "180500") }, emptyList())
        }
    }

    private val account = object : AccountRepository {
        override suspend fun getAccountBalance(): AccountStatus = throw UnsupportedOperationException()
        override suspend fun getBalance(ticker: String) = HoldingStock(ticker, ticker, "16", "146.08", "126", "-13", "2337")
        override suspend fun getPresentAccountBalance(): PresentAccountStatus = throw UnsupportedOperationException()
        override suspend fun getDailyProfit(ticker: String): List<Double> = emptyList()
        override suspend fun getTotalAsset(): TotalAsset = throw UnsupportedOperationException()
    }

    private fun statusRepo() = BacktestInvestmentStatusRepository().apply {
        initialize(
            InvestmentStatus(
                ticker = "SOXL", totalInvested = 2337.0, oneTimeAmount = 500.0, initialCapital = 20000.0,
                avgPrice = 146.08, quantity = 16, targetRate = 20.0, realizedTotalProfit = 0.0, updatedAt = ""
            )
        )
    }

    @Test
    fun `LOC 매수가는 호가의 125% 를 넘지 않는다`() = runBlocking {
        val stock = FakeStockRepository(previousClose = 127.57, live = 125.96)
        GenerateOrdersUseCase(stock, account, statusRepo(), BacktestTradeHistoryRepository())("SOXL")

        val cap = 157.45  // floor(min(127.57, 125.96) * 1.25, 2)
        val locBuys = stock.sent.filter { it.side == OrderSide.BUY && it.type == OrderType.LOC }
        assertTrue(locBuys.isNotEmpty())
        assertTrue(locBuys.all { it.price <= cap }, "상한 초과: ${locBuys.map { it.price }}")
        assertTrue(locBuys.any { it.price == cap }, "별% 매수가 상한으로 잘려야 한다: ${locBuys.map { it.price }}")
        assertTrue(locBuys.any { it.price == 146.08 }, "상한 아래인 평단 LOC 는 그대로: ${locBuys.map { it.price }}")
        // 매도는 건드리지 않는다
        assertTrue(stock.sent.filter { it.side == OrderSide.SELL }.none { it.price == cap })
    }

    @Test
    fun `호가가 전일 종가보다 높으면 전일 종가 기준으로 자른다`() = runBlocking {
        val stock = FakeStockRepository(previousClose = 120.0, live = 130.0)
        GenerateOrdersUseCase(stock, account, statusRepo(), BacktestTradeHistoryRepository())("SOXL")

        val locBuys = stock.sent.filter { it.side == OrderSide.BUY && it.type == OrderType.LOC }
        assertTrue(locBuys.all { it.price <= 150.0 }, "${locBuys.map { it.price }}")
    }

    @Test
    fun `접수 뒤 거래소가 거부한 주문은 REJECTED 와 사유로 고친다`() = runBlocking {
        val stock = FakeStockRepository(previousClose = 127.57, live = 125.96)
        val histories = BacktestTradeHistoryRepository()
        val rejecting = checker(mapOf("0001" to "[6008] Limit Price Too Aggressive"))

        GenerateOrdersUseCase(stock, account, statusRepo(), histories, rejecting)("SOXL")

        val rejected = histories.findByOrderNo("0001")!!
        assertEquals(OrderStatus.REJECTED, rejected.status)
        assertTrue(rejected.failReason!!.contains("Too Aggressive"))
        assertEquals(OrderStatus.PENDING, histories.findByOrderNo("0002")?.status)
    }
}
