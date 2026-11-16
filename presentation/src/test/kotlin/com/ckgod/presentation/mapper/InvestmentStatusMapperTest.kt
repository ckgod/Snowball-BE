package com.ckgod.presentation.mapper

import com.ckgod.domain.model.InvestmentStatus
import com.ckgod.domain.model.MarketPrice
import com.ckgod.domain.model.OrderRequest
import com.ckgod.domain.model.OrderSubmission
import com.ckgod.domain.repository.StockRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class InvestmentStatusMapperTest {

    private val status = InvestmentStatus(
        ticker = "SOXL", totalInvested = 10748.83, oneTimeAmount = 390.0, initialCapital = 11700.0, division = 30,
        avgPrice = 85.9906, quantity = 125, targetRate = 20.0, realizedTotalProfit = 1061.59, updatedAt = ""
    )

    private class Prices(private val price: MarketPrice?) : StockRepository {
        var includeDayMarket: Boolean? = null
        override suspend fun getCurrentPrice(stockCode: String, includeDayMarket: Boolean): MarketPrice? {
            this.includeDayMarket = includeDayMarket
            return price
        }
        override suspend fun getExchangeRate() = 1450.0
        override suspend fun postOrder(buyOrders: List<OrderRequest>, sellOrders: List<OrderRequest>): OrderSubmission =
            throw UnsupportedOperationException()
    }

    @Test
    fun `현재가를 주간거래 우선으로 조회해 응답을 만든다`() = runBlocking {
        val repo = Prices(
            MarketPrice(
                ticker = "SOXL", price = "53.79", previousClose = "58.32", changeRate = "-7.77", open = "0", high = "0",
                low = "0", volume = "0", krwPrice = "0", krwChangeAmount = "0", exchangeRate = "1335.8", isDayMarket = true
            )
        )

        val response = InvestmentStatusMapper.toResponse(status, repo)

        assertEquals(true, repo.includeDayMarket)
        assertEquals(InvestmentStatusMapper.toResponse(status, 53.79, -7.77, 1335.8, isDayMarket = true), response)
    }

    @Test
    fun `시세가 없으면 현재가 0 으로 만든다`() = runBlocking {
        val response = InvestmentStatusMapper.toResponse(status, Prices(null))

        assertEquals(InvestmentStatusMapper.toResponse(status, 0.0, 0.0, null, isDayMarket = false), response)
    }
}
