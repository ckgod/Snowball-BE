package com.ckgod.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class BacktestRequest(
    val ticker: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val initialCapital: Double,
    val oneTimeAmount: Double,
    val division: Int = 40,
    val targetRate: Double,
    val starMode: StarMode = StarMode.P1_2
)

@Serializable
data class BacktestResult(
    val request: BacktestRequest,
    val summary: BacktestSummary,
    val dailySnapshots: List<DailySnapshot>,
    val trades: List<BacktestTrade>
)

@Serializable
data class BacktestSummary(
    val totalTradingDays: Int,
    val totalOrders: Int,
    val filledOrders: Int,
    val totalInvested: Double,
    val finalQuantity: Int,
    val finalAvgPrice: Double,
    val finalTValue: Double,
    val realizedProfit: Double,
    val unrealizedProfit: Double,
    val availableCash: Double,
    val finalPortfolioValue: Double,
    val totalReturn: Double
)

@Serializable
data class DailySnapshot(
    val date: LocalDate,
    val closePrice: Double,
    val quantity: Int,
    val avgPrice: Double,
    val tValue: Double,
    val starPercent: Double,
    val totalInvested: Double,
    val portfolioValue: Double,
    val availableCash: Double
)

@Serializable
data class BacktestTrade(
    val date: LocalDate,
    val side: OrderSide,
    val type: OrderType,
    val orderPrice: Double,
    val filledPrice: Double,
    val quantity: Int,
    val tValue: Double,
    val crashRate: Double? = null
)
