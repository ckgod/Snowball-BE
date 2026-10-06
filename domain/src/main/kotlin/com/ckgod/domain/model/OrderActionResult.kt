package com.ckgod.domain.model

/**
 * 앱에서 요청한 주문 동작(신규·정정·취소)의 결과.
 *
 * @param orderNo 신규·정정으로 새로 생긴 KIS 주문번호
 */
data class OrderActionResult(
    val success: Boolean,
    val message: String,
    val orderNo: String? = null
)
