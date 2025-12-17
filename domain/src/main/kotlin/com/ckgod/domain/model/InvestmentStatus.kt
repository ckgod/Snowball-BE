package com.ckgod.domain.model

import java.time.LocalDateTime
import kotlin.math.ceil

data class InvestmentStatus(
    val ticker: String,              // 종목명 (예: TQQQ)
    val totalInvested: Double,        // 매수 누적액 (T값 분자)
    val oneTimeAmount: Double,        // 1회 매수금 (T값 분모)
    val initialCapital: Double,       // 원금
    val division: Int = 40,           // 분할 수
    val avgPrice: Double,             // 내 평단가
    val targetRate: Double,           // 오늘의 별% (목표 수익률)
    val buyLocPrice: Double,          // 오늘 매수 걸어둘 가격
    val sellLocPrice: Double,         // 오늘 매도 걸어둘 가격
    val updatedAt: String             // 마지막 갱신 시간
) {
    companion object {
        fun create(
            ticker: String,
            initialCapital: Double,
            division: Int = 40,
            targetRate: Double = 15.0
        ): InvestmentStatus {
            val oneTimeAmount = initialCapital / division
            val baseRate = when(ticker) {
                "TQQQ" -> 15.0
                "SOXL" -> 20.0
                else -> targetRate
            }
            return InvestmentStatus(
                ticker = ticker,
                totalInvested = 0.0,
                initialCapital = initialCapital,
                oneTimeAmount = oneTimeAmount, // TODO initialCapital에 의해 조절되게 수정
                division = division,
                avgPrice = 0.0,
                targetRate = baseRate,
                buyLocPrice = 0.0,
                sellLocPrice = 0.0,
                updatedAt = LocalDateTime.now().toString()
            )
        }
    }

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

    fun updateFromAccount(
        totalInvested: Double,
        avgPrice: Double,
        dailyProfit: Double
    ): InvestmentStatus {
        val newOneTimeAmount = if (dailyProfit > 0) {
            oneTimeAmount + (dailyProfit / (division * 2).toDouble())
        } else {
            oneTimeAmount
        }

        return copy(
            totalInvested = totalInvested,
            oneTimeAmount = newOneTimeAmount,
            avgPrice = avgPrice,
            updatedAt = LocalDateTime.now().toString()
        )
    }

    fun updateOrderPrices(
        currentPrice: Double
    ): InvestmentStatus {
        val buyPrice = currentPrice * (1.0 + targetRate / 100.0)
        val sellPrice = avgPrice * (1.0 + targetRate / 100.0)

        return copy(
            buyLocPrice = buyPrice,
            sellLocPrice = sellPrice,
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
