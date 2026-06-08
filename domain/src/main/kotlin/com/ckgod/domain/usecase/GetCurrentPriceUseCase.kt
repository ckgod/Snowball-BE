package com.ckgod.domain.usecase

import com.ckgod.domain.model.MarketPrice
import com.ckgod.domain.repository.StockRepository

class GetCurrentPriceUseCase(
    private val repository: StockRepository,
) {
    suspend operator fun invoke(stockCode: String): MarketPrice? {
        return repository.getCurrentPrice(
            stockCode = stockCode,
            includeDayMarket = true
        )
    }
}
