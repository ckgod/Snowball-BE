package com.ckgod.presentation.mapper

import com.ckgod.domain.model.InvestmentStatus
import com.ckgod.domain.model.TradePhase as DomainPhase
import com.ckgod.snowball.model.TradePhase as PresentationPhase
import com.ckgod.domain.utils.roundTo2Decimal
import com.ckgod.snowball.model.InvestmentStatusResponse

object InvestmentStatusMapper {
    fun toResponse(
        status: InvestmentStatus,
        currentPrice: Double,
        dailyChangeRate: Double,
        exchangeRate: Double?
    ): InvestmentStatusResponse {
        val rawProfitRate = if (status.avgPrice > 0) {
            ((currentPrice - status.avgPrice) / status.avgPrice) * 100.0
        } else {
            0.0
        }

        val profitRate = rawProfitRate.roundTo2Decimal()
        val starPercent = status.starPercent.roundTo2Decimal()
        val profitAmount = (status.quantity * (currentPrice - status.avgPrice)).roundTo2Decimal()

        val tradePhase = when(status.phase) {
            DomainPhase.FRONT_HALF -> PresentationPhase.FIRST_HALF
            DomainPhase.BACK_HALF -> PresentationPhase.BACK_HALF
            DomainPhase.QUARTER_MODE -> PresentationPhase.QUARTER_MODE
            DomainPhase.EXHAUSTED -> PresentationPhase.EXHAUSTED
        }

        return InvestmentStatusResponse(
            ticker = status.ticker,
            fullName = status.fullName,
            currentPrice = currentPrice,
            dailyChangeRate = dailyChangeRate,
            tValue = status.tValue,
            totalDivision = status.division,
            starPercent = starPercent,
            phase = tradePhase,
            avgPrice = status.avgPrice,
            quantity = status.quantity,
            profitRate = profitRate,
            profitAmount = profitAmount,
            oneTimeAmount = status.oneTimeAmount,
            totalInvested = status.totalInvested,
            exchangeRate = exchangeRate,
            capital = status.initialCapital,
            nextSellStarPrice = status.starSellPrice,
            nextSellTargetPrice = status.targetSellPrice,
            nextBuyStarPrice = status.getBuyPrice(currentPrice),
            realizedProfit = status.realizedTotalProfit
        )
    }
}
