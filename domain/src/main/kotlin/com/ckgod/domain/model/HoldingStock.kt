package com.ckgod.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class HoldingStock(
    val ticker: String,            // 티커 (TQQQ, SOXL)
    val name: String,              // 종목명
    val quantity: String,          // 보유 수량 (실수형, 미주는 소수점 가능)
    val avgPrice: String,          // 내 평단가
    val currentPrice: String,      // 현재가
    val profitRate: String,         // 수익률
    val investedAmount: String    // 매수 누적액
)
