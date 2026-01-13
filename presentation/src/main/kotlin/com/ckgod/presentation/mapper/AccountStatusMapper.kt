package com.ckgod.presentation.mapper

import com.ckgod.domain.model.HoldingStock
import com.ckgod.domain.model.PresentAccountStatus
import com.ckgod.domain.utils.roundTo2Decimal
import com.ckgod.snowball.model.AccountStatusResponse
import com.ckgod.snowball.model.HoldingStockResponse

object AccountStatusMapper {
    fun toResponse(
        accountStatus: PresentAccountStatus,
        capitalList: List<Pair<String, Double>>
    ): AccountStatusResponse {
        return AccountStatusResponse(
            totalAssetValueUsd = accountStatus.totalAssetValueUsd,
            totalBuyingValueUsd = accountStatus.totalBuyingValueUsd,
            totalEvalValueUsd = accountStatus.totalEvalValueUsd,
            totalProfitUsd = accountStatus.totalProfitUsd,
            totalProfitRate = accountStatus.totalProfitRate,
            totalCashUsd = accountStatus.totalCashUsd,
            orderableCashUsd = accountStatus.orderableCashUsd,
            lockedCashUsd = accountStatus.lockedCashUsd,
            holdingStocks = accountStatus.holdings.map { stock ->
                val capital = capitalList.find { it.first == stock.ticker }?.second ?: 0.0
                HoldingStockMapper.toResponse(stock, capital)
            }
        )
    }
}

object HoldingStockMapper {
    fun toResponse(
        stock: HoldingStock,
        capital: Double
    ) : HoldingStockResponse {
        return HoldingStockResponse(
            ticker = stock.ticker,
            name = stock.name,
            quantity = stock.quantity.toDouble().toInt(),
            avgPrice = stock.avgPrice.toDouble().roundTo2Decimal(),
            currentPrice = stock.currentPrice.toDouble().roundTo2Decimal(),
            profitRate = stock.profitRate.toDouble().roundTo2Decimal(),
            investedAmount = stock.investedAmount.toDouble().roundTo2Decimal(),
            capital = capital
        )
    }
}