package com.ckgod.domain.repository

import com.ckgod.domain.model.StockPriceHistory
import kotlinx.datetime.LocalDate

/**
 * 주식 가격 히스토리 Repository
 *
 * Yahoo Finance에서 수집한 과거 가격 데이터 관리
 * 백테스팅 및 과거 데이터 분석에 사용
 */
interface StockPriceHistoryRepository {
    /**
     * 특정 날짜의 가격 정보 조회
     */
    suspend fun getPriceByDate(ticker: String, date: LocalDate): StockPriceHistory?

    /**
     * 기간별 가격 정보 조회 (백테스팅용)
     */
    suspend fun getPriceRange(
        ticker: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<StockPriceHistory>

    /**
     * 최신 가격 정보 조회
     */
    suspend fun getLatestPrice(ticker: String): StockPriceHistory?

    /**
     * 모든 종목의 특정 날짜 가격 조회
     */
    suspend fun getAllPricesByDate(date: LocalDate): List<StockPriceHistory>

    /**
     * 특정 종목의 최근 N일 가격 조회
     */
    suspend fun getRecentPrices(ticker: String, days: Int): List<StockPriceHistory>
}
