package com.ckgod.domain.model

data class ExecutionInfo(
    val ticker: String,
    val orderNo: String,
    val filledPrice: Double,
    val filledQuantity: Double,
    val orderQuantity: Double,
    /** 거래소·KIS 가 접수 뒤에 거부한 주문이면 그 사유. 거부가 아니면 null */
    val rejectReason: String? = null
) {
    val isFullyFilled: Boolean
        get() = filledQuantity == orderQuantity

    val isPartiallyFilled: Boolean
        get() = filledQuantity > 0 && filledQuantity < orderQuantity

    val isRejected: Boolean
        get() = rejectReason != null
}
