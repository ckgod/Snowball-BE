package com.ckgod.domain.model

import com.ckgod.domain.utils.roundTo2Decimal
import java.time.LocalDateTime
import kotlin.math.ceil

data class InvestmentStatus(
    val ticker: String,              // 종목명 (예: TQQQ)
    val fullName: String? = null,    // 종목 풀네임
    val totalInvested: Double,       // 매수 누적액 (T값 분자)
    val oneTimeAmount: Double,       // 1회 매수금 (T값 분모)
    val initialCapital: Double,      // 원금
    val division: Int = 40,          // 분할 수
    val avgPrice: Double,            // 내 평단가
    val quantity: Int = 0,           // 보유 수량
    val targetRate: Double,          // 기준 % (목표 수익률)
    val realizedTotalProfit: Double, // 총 실현 손익
    val updatedAt: String,           // 마지막 갱신 시간
) {

    /**
     * T 값
     * 누적 매수금 / 1회 매수액
     */
    val tValue : Double
        get() {
            if (oneTimeAmount == 0.0) return 0.0
            val rawT = totalInvested / oneTimeAmount
            return ceil(rawT * 100) / 100.0
        }

    /**
     * 별% 계산: targetRate × (1 - 2T / division)
     * T = division / 2일 때 0%가 됨
     */
    val starPercent: Double
        get() = targetRate * (1.0 - (2.0 * tValue / division))

    val phase: TradePhase
        get() = when {
            tValue <= (division / 2).toDouble() -> TradePhase.FRONT_HALF
            tValue < (division - 1).toDouble() -> TradePhase.BACK_HALF
            tValue < division.toDouble() -> TradePhase.QUARTER_MODE
            else -> TradePhase.EXHAUSTED
        }

    val exchange: Exchange get() = when(ticker) {
        "TQQQ" -> Exchange.NASD
        "SOXL", "FNGU" -> Exchange.AMEX
        else -> Exchange.NASD
    }

    val starSellPrice: Double
        get() = (avgPrice * (1.0 + starPercent / 100.0)).roundTo2Decimal()

    val targetSellPrice: Double
        get() = (avgPrice * (1.0 + targetRate / 100.0)).roundTo2Decimal()

    val starBuyPrice: Double
        get() = (avgPrice * (1.0 + starPercent / 100.0)).roundTo2Decimal()

    fun getBuyPrice(currentPrice: Double): Double {
        return if (avgPrice == 0.0) {
            (currentPrice * (1.0 + starPercent / 100.0)).roundTo2Decimal()
        } else {
            starBuyPrice
        }
    }

    fun updateFromAccount(
        name: String?,
        totalInvested: Double,
        avgPrice: Double,
        quantity: Int,
        dailyProfit: Double
    ): InvestmentStatus {
        val newOneTimeAmount = if (dailyProfit > 0) {
            oneTimeAmount + (dailyProfit / (division * 2).toDouble())
        } else {
            oneTimeAmount
        }

        return copy(
            fullName = name ?: fullName,
            totalInvested = totalInvested,
            oneTimeAmount = newOneTimeAmount,
            avgPrice = avgPrice,
            quantity = quantity,
            realizedTotalProfit = realizedTotalProfit + dailyProfit,
            updatedAt = LocalDateTime.now().toString()
        )
    }
}

enum class TradePhase(val description: String) {
    FRONT_HALF("전반전"),
    BACK_HALF("후반전"),
    QUARTER_MODE("쿼터모드"),
    EXHAUSTED("자금소진")
}
