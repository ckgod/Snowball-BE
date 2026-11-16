package com.ckgod.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class HoldingStock(
    val ticker: String,
    val name: String,
    val quantity: Double,          // 보유 수량 (미국 주식은 소수점 보유가 있을 수 있다)
    val avgPrice: Double,          // 평단가 (USD)
    val currentPrice: Double,      // 현재가 (USD)
    val profitRate: Double,        // 수익률 (%)
    val investedAmount: Double     // 매수 누적액 (USD)
)
