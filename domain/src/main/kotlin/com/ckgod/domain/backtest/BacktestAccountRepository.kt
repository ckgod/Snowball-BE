package com.ckgod.domain.backtest

import com.ckgod.domain.model.*
import com.ckgod.domain.repository.AccountRepository

class BacktestAccountRepository(
    initialCash: Double = 0.0
) : AccountRepository {

    private val holdings = mutableMapOf<String, HoldingStock>()
    var availableCash: Double = initialCash
        private set

    fun deductCash(amount: Double): Boolean {
        if (availableCash < amount) return false
        availableCash -= amount
        return true
    }

    fun addCash(amount: Double) {
        availableCash += amount
    }

    override suspend fun getAccountBalance(): AccountStatus {
        return AccountStatus(
            totalPurchaseAmount = "0",
            totalEvaluationAmount = "0",
            totalProfitOrLoss = "0",
            totalProfitRate = "0",
            holdings = holdings.values.toList()
        )
    }

    override suspend fun getPresentAccountBalance(): PresentAccountStatus {
        return PresentAccountStatus(
            totalAssetValueUsd = 0.0,
            totalBuyingValueUsd = 0.0,
            totalEvalValueUsd = 0.0,
            totalProfitUsd = 0.0,
            totalProfitRate = 0.0,
            totalCashUsd = 0.0,
            orderableCashUsd = 0.0,
            lockedCashUsd = 0.0,
            holdings = holdings.values.toList()
        )
    }

    override suspend fun getDailyProfit(ticker: String): List<Double> = emptyList()

    override suspend fun getTotalAsset(): TotalAsset = TotalAsset(assets = emptyList())

    fun updateHolding(ticker: String, quantity: Int, avgPrice: Double, investedAmount: Double) {
        if (quantity <= 0) {
            holdings.remove(ticker)
        } else {
            holdings[ticker] = HoldingStock(
                ticker = ticker,
                name = ticker,
                quantity = quantity.toString(),
                avgPrice = "%.2f".format(avgPrice),
                currentPrice = "0",
                profitRate = "0",
                investedAmount = "%.2f".format(investedAmount)
            )
        }
    }

    fun getHolding(ticker: String): HoldingStock? = holdings[ticker]
}
