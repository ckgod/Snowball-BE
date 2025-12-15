package com.ckgod.domain.repository

import com.ckgod.domain.model.TradeHistory

/**
 * 거래 히스토리 Repository
 */
interface TradeHistoryRepository {
    /**
     * 거래 내역 저장
     */
    suspend fun save(history: TradeHistory): TradeHistory

    /**
     * 모든 거래 내역 조회 (최신순, 페이징)
     */
    suspend fun findAll(limit: Int = 100): List<TradeHistory>
}
