package com.ckgod.presentation.response

import kotlinx.serialization.Serializable

@Serializable
data class HistoryResponse(
    val total: Int,
    val histories: List<HistoryItem>
)

@Serializable
data class HistoryItem(
    val id: Long,
    val ticker: String,

    // 주문 정보
    val orderNo: String?,
    val orderSide: String,
    val orderType: String,
    val orderPrice: Double,
    val orderQuantity: Int,
    val orderTime: String,

    // 체결 정보
    val status: String,
    val filledQuantity: Int,
    val filledPrice: Double,
    val filledTime: String?,

    // 전략 정보
    val tValue: Double,

    // 메타 정보
    val createdAt: String
)