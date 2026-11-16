package com.ckgod.domain.usecase

import com.ckgod.domain.backtest.BacktestInvestmentStatusRepository
import com.ckgod.domain.backtest.BacktestTradeHistoryRepository
import com.ckgod.domain.model.*
import com.ckgod.domain.repository.AccountRepository
import com.ckgod.domain.repository.ExecutionRepository
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncStrategyUseCaseTest {

    private val soxl = InvestmentStatus(
        ticker = "SOXL", totalInvested = 10748.83, oneTimeAmount = 390.0, initialCapital = 11700.0,
        division = 30, avgPrice = 85.9906, quantity = 125, targetRate = 20.0, realizedTotalProfit = 1061.59,
        updatedAt = "", starMode = StarMode.P1_2
    )

    private class FailingAccount : AccountRepository {
        override suspend fun getAccountBalance(): AccountStatus = throw IllegalStateException("잔고 조회 실패 [EGW00201]")
        override suspend fun getPresentAccountBalance(): PresentAccountStatus = throw UnsupportedOperationException()
        override suspend fun getDailyProfit(ticker: String): List<Double> = emptyList()
        override suspend fun getTotalAsset(): TotalAsset = throw UnsupportedOperationException()
    }

    private class Executions(private val list: List<ExecutionInfo>) : ExecutionRepository {
        override suspend fun getExecutionList(startDate: String, endDate: String) = list
    }

    @Test
    fun `잔고 조회가 실패하면 투자 상태를 덮어쓰지 않는다`() = runBlocking {
        val statuses = BacktestInvestmentStatusRepository().apply { initialize(soxl) }
        val useCase = SyncStrategyUseCase(FailingAccount(), statuses, Executions(emptyList()), BacktestTradeHistoryRepository())

        val results = useCase()

        assertTrue(results.isEmpty())
        assertEquals(soxl, statuses.get("SOXL"))
    }

    @Test
    fun `평단을 모르는 매도 체결은 실현 손익으로 잡지 않는다`() = runBlocking {
        val histories = BacktestTradeHistoryRepository()
        histories.save(sell(orderNo = "0001", avgPrice = 0.0))
        histories.save(sell(orderNo = "0002", avgPrice = 80.0))
        val executions = Executions(listOf(filled("0001", price = 100.0, qty = 10.0), filled("0002", price = 100.0, qty = 10.0)))
        val useCase = SyncStrategyUseCase(FailingAccount(), BacktestInvestmentStatusRepository(), executions, histories)

        useCase()

        assertEquals(0.0, histories.findByOrderNo("0001")!!.realizedProfitAmount)
        assertEquals(200.0, histories.findByOrderNo("0002")!!.realizedProfitAmount)
    }

    private fun sell(orderNo: String, avgPrice: Double) = TradeHistory(
        ticker = "SOXL", orderNo = orderNo, orderSide = OrderSide.SELL, orderType = OrderType.LIMIT,
        orderPrice = 100.0, orderQuantity = 10, orderTime = LocalDateTime.now(), tValue = 27.0, avgPrice = avgPrice
    )

    private fun filled(orderNo: String, price: Double, qty: Double) =
        ExecutionInfo(ticker = "SOXL", orderNo = orderNo, filledPrice = price, filledQuantity = qty, orderQuantity = qty)
}
