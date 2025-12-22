package com.ckgod.domain.model

import java.time.LocalDateTime

data class TradeHistory(
    val id: Long = 0,                           // PK (auto increment)
    val ticker: String,                         // 종목명

    // 주문 정보
    val orderNo: String? = null,                // KIS 주문번호
    val orderSide: OrderSide,                   // BUY, SELL
    val orderType: OrderType,                   // LIMIT, MOC, LOC
    val orderPrice: Double,                     // 주문 가격
    val orderQuantity: Int,                     // 주문 수량
    val orderTime: LocalDateTime,               // 주문 시각

    // 체결 정보 (나중에 업데이트)
    val status: OrderStatus = OrderStatus.PENDING,  // 주문 상태
    val filledQuantity: Int = 0,                // 체결된 수량
    val filledPrice: Double = 0.0,              // 체결 평균 가격
    val filledTime: LocalDateTime? = null,      // 체결 시각

    // 전략 정보
    val tValue: Double,                         // 주문 당시 T값

    // 메타 정보
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    val isFullyFilled: Boolean
        get() = status == OrderStatus.FILLED && filledQuantity == orderQuantity

    val isPartiallyFilled: Boolean
        get() = status == OrderStatus.PARTIAL && filledQuantity > 0 && filledQuantity < orderQuantity
}

/**
 * 주문 상태
 */
enum class OrderStatus {
    PENDING,    // 주문 접수 (체결 대기 중)
    FILLED,     // 전량 체결
    PARTIAL,    // 부분 체결
    CANCELED    // 주문 취소
}
