package com.ckgod.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AccountStatus(
    val totalPurchaseAmount: String, // 총 매수 금액 (USD)
    val totalEvaluationAmount: String, // 총 평가 금액 (USD)
    val totalProfitOrLoss: String, // 총 손익 (USD)
    val totalProfitRate: String, // 총 수익률 (%)
    val holdings: List<HoldingStock> // 보유 종목 리스트
)

@Serializable
data class PresentAccountStatus(
    val totalAssetValueUsd: Double,    // 총 자산(달러 환산)
    val totalBuyingValueUsd: Double,   // 총 매입금(달러)
    val totalEvalValueUsd: Double,     // 총 평가금(달러)
    val totalProfitUsd: Double,        // 총 평가손익($)
    val totalProfitRate: Double,       // 총 수익률(%)

    val totalCashUsd: Double,          // 외화예수금
    val orderableCashUsd: Double,      // 주문가능금액
    val lockedCashUsd: Double,         // 묶인 돈(증거금)

    val holdings: List<HoldingStock>    // 보유 종목 리스트
)