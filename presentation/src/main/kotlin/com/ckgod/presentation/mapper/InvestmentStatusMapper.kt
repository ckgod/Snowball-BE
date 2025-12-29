package com.ckgod.presentation.mapper

import com.ckgod.domain.model.InvestmentStatus
import com.ckgod.domain.utils.roundTo2Decimal
import com.ckgod.snowball.model.InvestmentStatusUiModel

object InvestmentStatusMapper {
    fun toUiModel(
        status: InvestmentStatus,
        currentPrice: Double,
        dailyChangeRate: Double,
        exchangeRate: Double?
    ): InvestmentStatusUiModel {
        val rawProfitRate = if (status.avgPrice > 0) {
            ((currentPrice - status.avgPrice) / status.avgPrice) * 100.0
        } else {
            0.0
        }

        val profitRate = rawProfitRate.roundTo2Decimal()
        val starPercent = status.starPercent.roundTo2Decimal()
        val profitAmount = (status.quantity * (currentPrice - status.avgPrice)).roundTo2Decimal()

        return InvestmentStatusUiModel(
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
            capital = status.initialCapital,
            nextSellStarPrice = status.starSellPrice,
            nextSellTargetPrice = status.targetSellPrice,
            nextBuyStarPrice = status.getBuyPrice(currentPrice),
            realizedProfit = status.realizedTotalProfit
        )
    }
}
