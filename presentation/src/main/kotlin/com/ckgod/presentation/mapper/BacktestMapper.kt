package com.ckgod.presentation.mapper

import com.ckgod.domain.model.BacktestRequest as DomainBacktestRequest
import com.ckgod.domain.model.BacktestResult as DomainBacktestResult
import com.ckgod.domain.model.BacktestTrade as DomainBacktestTrade
import com.ckgod.domain.model.DailySnapshot as DomainDailySnapshot
import com.ckgod.domain.model.BacktestSummary as DomainBacktestSummary
import com.ckgod.domain.model.StarMode as DomainStarMode
import com.ckgod.domain.model.OrderSide as DomainOrderSide
import com.ckgod.domain.model.OrderType as DomainOrderType
import com.ckgod.snowball.model.*

object BacktestMapper {

    fun toDomain(request: BacktestRequest): DomainBacktestRequest {
        return DomainBacktestRequest(
            ticker = request.ticker,
            startDate = request.startDate,
            endDate = request.endDate,
            initialCapital = request.initialCapital,
            oneTimeAmount = request.oneTimeAmount,
            division = request.division,
            targetRate = request.targetRate,
            starMode = when (request.starMode) {
                StarMode.P1_2 -> DomainStarMode.P1_2
                StarMode.P2_3 -> DomainStarMode.P2_3
            }
        )
    }

    fun toResponse(result: DomainBacktestResult): BacktestResponse {
        return BacktestResponse(
            request = fromDomainRequest(result.request),
            summary = fromDomainSummary(result.summary),
            dailySnapshots = result.dailySnapshots.map { fromDomainSnapshot(it) },
            trades = result.trades.map { fromDomainTrade(it) }
        )
    }

    private fun fromDomainRequest(request: DomainBacktestRequest): BacktestRequest {
        return BacktestRequest(
            ticker = request.ticker,
            startDate = request.startDate,
            endDate = request.endDate,
            initialCapital = request.initialCapital,
            oneTimeAmount = request.oneTimeAmount,
            division = request.division,
            targetRate = request.targetRate,
            starMode = when (request.starMode) {
                DomainStarMode.P1_2 -> StarMode.P1_2
                DomainStarMode.P2_3 -> StarMode.P2_3
            }
        )
    }

    private fun fromDomainSummary(summary: DomainBacktestSummary): BacktestSummary {
        return BacktestSummary(
            totalTradingDays = summary.totalTradingDays,
            totalOrders = summary.totalOrders,
            filledOrders = summary.filledOrders,
            totalInvested = summary.totalInvested,
            finalQuantity = summary.finalQuantity,
            finalAvgPrice = summary.finalAvgPrice,
            finalTValue = summary.finalTValue,
            realizedProfit = summary.realizedProfit,
            unrealizedProfit = summary.unrealizedProfit,
            availableCash = summary.availableCash,
            finalPortfolioValue = summary.finalPortfolioValue,
            totalReturn = summary.totalReturn
        )
    }

    private fun fromDomainSnapshot(snapshot: DomainDailySnapshot): BacktestDailySnapshot {
        return BacktestDailySnapshot(
            date = snapshot.date,
            closePrice = snapshot.closePrice,
            quantity = snapshot.quantity,
            avgPrice = snapshot.avgPrice,
            tValue = snapshot.tValue,
            starPercent = snapshot.starPercent,
            totalInvested = snapshot.totalInvested,
            portfolioValue = snapshot.portfolioValue,
            availableCash = snapshot.availableCash
        )
    }

    private fun fromDomainTrade(trade: DomainBacktestTrade): BacktestTradeResponse {
        return BacktestTradeResponse(
            date = trade.date,
            side = when (trade.side) {
                DomainOrderSide.BUY -> OrderSide.BUY
                DomainOrderSide.SELL -> OrderSide.SELL
            },
            type = when (trade.type) {
                DomainOrderType.LOC -> OrderType.LOC
                DomainOrderType.LIMIT -> OrderType.LIMIT
                DomainOrderType.MOC -> OrderType.MOC
            },
            orderPrice = trade.orderPrice,
            filledPrice = trade.filledPrice,
            quantity = trade.quantity,
            tValue = trade.tValue,
            crashRate = trade.crashRate
        )
    }
}
