package com.ckgod.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AccountStatus(
    val holdings: List<HoldingStock>
)

@Serializable
data class PresentAccountStatus(
    val totalAssetValueUsd: Double,    // 총 자산(달러 환산)
    val totalBuyingValueUsd: Double,   // 총 매입금(달러)
    val totalEvalValueUsd: Double,     // 총 평가금(달러)
    val totalProfitUsd: Double,        // 총 평가손익($)
    val totalProfitRate: Double,       // 총 수익률(%)

    val totalCashUsd: Double,          // 외화예수금
    val orderableCashUsd: Double,      // 체결 기준 현금 (예수금 - 미결제 매수 + 미결제 매도)
    val lockedCashUsd: Double,         // 묶인 돈(증거금)

    val holdings: List<HoldingStock>    // 보유 종목 리스트
)