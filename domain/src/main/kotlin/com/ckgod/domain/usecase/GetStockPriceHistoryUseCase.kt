package com.ckgod.domain.usecase

import com.ckgod.domain.model.StockPriceHistory
import com.ckgod.domain.repository.StockPriceHistoryRepository
import kotlinx.datetime.LocalDate

/**
 * 주식 가격 히스토리 조회 UseCase
 *
 * 백테스팅 및 과거 데이터 분석용
 */
class GetStockPriceHistoryUseCase(
    private val stockPriceHistoryRepository: StockPriceHistoryRepository
) {
    /**
     * 기간별 가격 정보 조회
     */
    suspend operator fun invoke(
        ticker: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<StockPriceHistory> {
        return stockPriceHistoryRepository.getPriceRange(ticker, startDate, endDate)
    }

    /**
     * 최근 N일 가격 정보 조회
     */
    suspend fun getRecentPrices(ticker: String, days: Int): List<StockPriceHistory> {
        return stockPriceHistoryRepository.getRecentPrices(ticker, days)
    }

    /**
     * 최신 가격 정보 조회
     */
    suspend fun getLatestPrice(ticker: String): StockPriceHistory? {
        return stockPriceHistoryRepository.getLatestPrice(ticker)
    }
}
