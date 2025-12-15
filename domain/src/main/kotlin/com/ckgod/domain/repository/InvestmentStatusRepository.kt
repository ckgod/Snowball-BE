package com.ckgod.domain.repository

import com.ckgod.domain.model.InvestmentStatus

/**
 * 투자 상태 Repository
 *
 * investment_status 테이블은 ticker별로 행 존재 (TQQQ, SOXL 등)
 */
interface InvestmentStatusRepository {
    /**
     * 모든 투자 중인 종목 조회
     */
    suspend fun findAll(): List<InvestmentStatus>

    /**
     * 특정 ticker의 상태 조회
     */
    suspend fun get(ticker: String): InvestmentStatus?

    /**
     * 상태 저장/업데이트
     */
    suspend fun save(status: InvestmentStatus): InvestmentStatus
}
