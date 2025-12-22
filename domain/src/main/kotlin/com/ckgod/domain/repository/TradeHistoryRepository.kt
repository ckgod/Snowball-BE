package com.ckgod.domain.repository

import com.ckgod.domain.model.OrderStatus
import com.ckgod.domain.model.TradeHistory
import java.time.LocalDateTime

/**
 * 주문/거래 히스토리 Repository
 */
interface TradeHistoryRepository {
    /**
     * 주문 내역 저장
     */
    suspend fun save(history: TradeHistory): TradeHistory

    /**
     * 주문 상태 업데이트 (체결 정보 업데이트)
     */
    suspend fun updateOrderStatus(
        orderNo: String,
        status: OrderStatus,
        filledQuantity: Int,
        filledPrice: Double,
        filledTime: LocalDateTime
    )

    /**
     * 주문번호로 조회
     */
    suspend fun findByOrderNo(orderNo: String): TradeHistory?

    /**
     * 특정 종목의 주문 내역 조회 (최신순)
     */
    suspend fun findByTicker(ticker: String, limit: Int = 100): List<TradeHistory>

    /**
     * 모든 주문 내역 조회 (최신순, 페이징)
     */
    suspend fun findAll(limit: Int = 100): List<TradeHistory>

    /**
     * PENDING 상태인 주문 조회 (체결 확인용)
     */
    suspend fun findPendingOrders(): List<TradeHistory>
}
