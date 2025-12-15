package com.ckgod.domain.model

import java.time.LocalDateTime

/**
 * 투자 현재 상태 (단일 행)
 *
 * 앱의 메인 화면에 표시될 "오늘의 성적표"
 */
data class InvestmentStatus(
    val ticker: String,              // 종목명 (예: TQQQ)
    val currentT: Double,             // 현재 T값 (핵심 지표)
    val totalInvested: Double,        // 매수 누적액 (T값 분자)
    val oneTimeAmount: Double,        // 1회 매수금 (T값 분모)
    val avgPrice: Double,             // 내 평단가
    val targetRate: Double,           // 오늘의 별% (목표 수익률)
    val buyLocPrice: Double,          // 오늘 매수 걸어둘 가격
    val sellLocPrice: Double,         // 오늘 매도 걸어둘 가격
    val updatedAt: String             // 마지막 갱신 시간
) {
    companion object {
        /**
         * 초기 상태 생성
         */
        fun create(
            ticker: String,
            initialCapital: Double
        ): InvestmentStatus {
            val oneTimeAmount = initialCapital / 80.0  // 80분할
            return InvestmentStatus(
                ticker = ticker,
                currentT = 0.0,
                totalInvested = 0.0,
                oneTimeAmount = oneTimeAmount,
                avgPrice = 0.0,
                targetRate = 15.0,  // 15 - (0.75 * 0) = 15%
                buyLocPrice = 0.0,
                sellLocPrice = 0.0,
                updatedAt = LocalDateTime.now().toString()
            )
        }
    }

    /**
     * 별% 계산: 15 - (0.75 × T)
     */
    fun calculateTargetRate(): Double {
        return 15.0 - (0.75 * currentT)
    }

    /**
     * 상태 업데이트 (정산 시)
     */
    fun updateFromAccount(
        totalInvested: Double,
        avgPrice: Double,
        dailyProfit: Double
    ): InvestmentStatus {
        // 수익 발생 시 1회 매수금 증가
        val newOneTimeAmount = if (dailyProfit > 0) {
            oneTimeAmount + (dailyProfit / 80.0)
        } else {
            oneTimeAmount
        }

        val newT = if (newOneTimeAmount > 0) {
            totalInvested / newOneTimeAmount
        } else {
            0.0
        }

        val newTargetRate = 15.0 - (0.75 * newT)

        return copy(
            totalInvested = totalInvested,
            oneTimeAmount = newOneTimeAmount,
            avgPrice = avgPrice,
            currentT = newT,
            targetRate = newTargetRate,
            updatedAt = LocalDateTime.now().toString()
        )
    }

    /**
     * 주문 가격 업데이트 (주문 생성 시)
     */
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
