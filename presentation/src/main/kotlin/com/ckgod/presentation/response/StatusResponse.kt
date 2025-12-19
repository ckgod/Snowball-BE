package com.ckgod.presentation.response

import com.ckgod.domain.model.InvestmentStatus
import kotlinx.serialization.Serializable
import kotlin.math.round

@Serializable
data class StatusResponse(
    val ticker: String,                    // 티커명 (예: TQQQ)
    val fullName: String?,                 // 종목 풀네임
    val currentPrice: Double,              // 현재가
    val dailyChangeRate: Double,           // 전일 대비 등락률 (%)

    // 전략 상태
    val tValue: Double,                    // 현재 T값
    val totalDivision: Int,                // 전체 분할 수 (예: 40)
    val starPercent: Double,               // 별 % (소수점 2자리)
    val phase: String,                     // 현재 구간 (전반전, 후반전 등)

    // 내 계좌 상태
    val avgPrice: Double,                  // 평균 단가
    val quantity: Int,                     // 보유 수량
    val profitRate: Double,                // 수익률 % (소수점 2자리)
    val profitAmount: Double,              // 평가 손익 금액
    val oneTimeAmount: Double,             // 1회 매수액
    val totalInvested: Double,             // 누적 투자 금액

    // 환율
    val exchangeRate: Double?,              // 환율
    val capital: Double?                   // 원금
) {
    companion object {
        fun from(
            status: InvestmentStatus,
            currentPrice: Double,
            dailyChangeRate: Double,
            exchangeRate: Double?
        ): StatusResponse {
            val rawProfitRate = if (status.avgPrice > 0) {
                ((currentPrice - status.avgPrice) / status.avgPrice) * 100.0
            } else {
                0.0
            }

            // 소수점 둘째자리로 반올림
            val profitRate = round(rawProfitRate * 100) / 100.0
            val starPercent = round(status.starPercent * 100) / 100.0
            val profitAmount = status.quantity * (currentPrice - status.avgPrice)

            return StatusResponse(
                ticker = status.ticker,
                fullName = status.fullName,
                currentPrice = currentPrice,
                dailyChangeRate = dailyChangeRate,
                tValue = status.tValue,
                totalDivision = status.division,
                starPercent = starPercent,
                phase = status.phase.description,
                avgPrice = status.avgPrice,
                quantity = status.quantity,
                profitRate = profitRate,
                profitAmount = profitAmount,
                oneTimeAmount = status.oneTimeAmount,
                totalInvested = status.totalInvested,
                exchangeRate = exchangeRate,
                capital = status.initialCapital
            )
        }
    }
}

@Serializable
data class StatusListResponse(
    val total: Int,
    val statusList: List<StatusResponse>
)