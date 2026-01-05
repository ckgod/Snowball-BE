package com.ckgod.domain.model

data class ExecutionInfo(
    val ticker: String,
    val orderNo: String,
    val filledPrice: Double,
    val filledQuantity: Double,
    val orderQuantity: Double
) {
    val isFullyFilled: Boolean
        get() = filledQuantity == orderQuantity

    val isPartiallyFilled: Boolean
        get() = filledQuantity > 0 && filledQuantity < orderQuantity
}