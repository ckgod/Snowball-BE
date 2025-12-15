package com.ckgod.domain.repository

import com.ckgod.domain.model.MarketPrice

interface StockRepository {
    suspend fun getStockPrice(userId: String, exchange: String, stockCode: String): MarketPrice?

    /**
     * 간단한 현재가 조회 (기본 exchange는 NASD)
     */
    suspend fun getCurrentPrice(ticker: String): MarketPrice {
        return getStockPrice("default", "NASD", ticker)
            ?: throw IllegalStateException("$ticker 현재가 조회 실패")
    }
}
