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

    override suspend fun getTotalAsset(): TotalAsset = TotalAsset(assets = emptyList())

    fun updateHolding(ticker: String, quantity: Int, avgPrice: Double, investedAmount: Double) {
        if (quantity <= 0) {
            holdings.remove(ticker)
        } else {
            // KIS 잔고처럼 평단·매수액을 소수 둘째 자리까지만 들고 있는다
            holdings[ticker] = HoldingStock(
                ticker = ticker,
                name = ticker,
                quantity = quantity.toDouble(),
                avgPrice = "%.2f".format(avgPrice).toDouble(),
                currentPrice = 0.0,
                profitRate = 0.0,
                investedAmount = "%.2f".format(investedAmount).toDouble()
            )
        }
    }

    fun getHolding(ticker: String): HoldingStock? = holdings[ticker]
}
