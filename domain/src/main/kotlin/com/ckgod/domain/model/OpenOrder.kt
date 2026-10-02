package com.ckgod.domain.model

/**
 * KIS 에 접수되어 아직 체결되지 않은 주문.
 */
data class OpenOrder(
    val orderNo: String,
    val originalOrderNo: String?,
    val ticker: String,
    val exchange: String,
    val side: OrderSide,
    val orderPrice: Double,
    val orderQuantity: Int,
    val filledQuantity: Int,
    val unfilledQuantity: Int,
    val orderDate: String,
    val orderTime: String
)
