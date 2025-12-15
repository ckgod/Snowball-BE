package com.ckgod.domain.model

/**
 * 거래 히스토리
 *
 * 앱의 상세 내역에 표시될 거래 기록
 */
data class TradeHistory(
    val id: Long,                    // PK
    val date: String,                 // 거래 일자 (2024-12-15)
    val type: TradeType,              // BUY / SELL / QUARTER_SELL
    val price: Double,                // 체결 가격
    val quantity: Int,                // 체결 수량
    val profit: Double,               // 실현 수익금 (매수 시 0)
    val tValueAt: Double              // 당시 T값 (기록용)
)

/**
 * 거래 유형
 */
enum class TradeType {
    BUY,           // 일반 매수
    SELL,          // 익절 매도
    QUARTER_SELL   // 쿼터 매도
}
