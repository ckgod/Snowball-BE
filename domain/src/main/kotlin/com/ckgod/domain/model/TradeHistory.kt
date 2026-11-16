package com.ckgod.domain.model

import java.time.LocalDateTime

data class TradeHistory(
    val id: Long = 0,                           // PK (auto increment)
    val ticker: String,                         // 종목명

    // 주문 정보
    val orderNo: String,                        // KIS 주문번호
    val orderSide: OrderSide,                   // BUY, SELL
    val orderType: OrderType,                   // LIMIT, MOC, LOC
    val orderPrice: Double,                     // 주문 가격
    val orderQuantity: Int,                     // 주문 수량
    val orderTime: LocalDateTime,               // 주문 시각
    val crashRate: Double? = null,              // 폭락 비율 %

    // 체결 정보 (나중에 업데이트)
    val status: OrderStatus = OrderStatus.PENDING,  // 주문 상태
    val filledQuantity: Int = 0,                // 체결된 수량
    val filledPrice: Double = 0.0,              // 체결 평균 가격
    val filledTime: LocalDateTime? = null,      // 체결 시각
    val avgPrice: Double = 0.0,                 // 체결 당시 평단가
    val realizedProfitAmount: Double = 0.0,     // 매도 체결 시 손익
    val failReason: String? = null,             // REJECTED 일 때 실패 사유
    val isManual: Boolean = false,              // 앱에서 직접 넣은 주문

    // 전략 정보
    val tValue: Double,                         // 주문 당시 T값

    // 메타 정보
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        /**
         * 주문 한 건의 전송 결과를 기록으로 만든다. 자동·수동 주문이 같은 규칙을 쓴다.
         *
         * - 접수: PENDING. [exchangeRejectReason] 이 있으면 주문번호를 받은 뒤 거래소가 거부한 것이라 REJECTED
         * - 접수 실패: 주문번호 없이 REJECTED. 체결 동기화는 주문번호로만 갱신하므로 이 행은 건드리지 않는다
         * - 매도는 체결 시 실현 손익 계산에 쓰도록 [sellAvgPrice] 를 남긴다
         */
        fun of(
            outcome: OrderOutcome,
            tValue: Double,
            sellAvgPrice: Double,
            orderTime: LocalDateTime,
            isManual: Boolean = false,
            exchangeRejectReason: String? = null
        ): TradeHistory {
            val request = outcome.request
            val (orderNo, failReason) = when (outcome) {
                is OrderResponse -> outcome.orderNo to exchangeRejectReason
                is OrderRejection -> "" to outcome.reason
            }
            return TradeHistory(
                ticker = request.ticker,
                orderNo = orderNo,
                orderSide = request.side,
                orderType = request.type,
                orderPrice = request.price,
                orderQuantity = request.quantity,
                orderTime = orderTime,
                crashRate = request.crashRate,
                status = if (failReason == null) OrderStatus.PENDING else OrderStatus.REJECTED,
                avgPrice = if (request.side == OrderSide.SELL) sellAvgPrice else 0.0,
                failReason = failReason,
                isManual = isManual,
                tValue = tValue
            )
        }
    }
}

/**
 * 주문 상태
 */
enum class OrderStatus {
    PENDING,    // 주문 접수 (체결 대기 중)
    FILLED,     // 전량 체결
    PARTIAL,    // 부분 체결
    CANCELED,   // 주문 취소
    REJECTED    // 주문 실패. 접수 전 실패(KIS 거부·전송 오류)는 orderNo 빈 문자열, 접수 뒤 거래소 거부는 주문번호 있음
}
