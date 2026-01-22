package com.ckgod.presentation.mapper

import com.ckgod.domain.model.HoldingStock
import com.ckgod.domain.model.PresentAccountStatus
import com.ckgod.domain.model.TotalAsset
import com.ckgod.domain.utils.roundTo2Decimal
import com.ckgod.snowball.model.AssetItemResponse
import com.ckgod.snowball.model.AssetType
import com.ckgod.snowball.model.HoldingStockResponse
import com.ckgod.snowball.model.TotalAssetResponse

object TotalAssetMapper {
    fun toResponse(
        accountStatus: PresentAccountStatus,
        totalAsset: TotalAsset,
        capitalList: List<Pair<String, Double>>,
        exchangeRate: Double,
    ): TotalAssetResponse {
        val assets = totalAsset.assets.mapIndexed { index, assetItem ->
            val assetTypes = AssetType.entries.toTypedArray()

            AssetItemResponse(
                type = assetTypes[index],
                purchaseAmount = (assetItem.purchaseAmount / exchangeRate).roundTo2Decimal(),
                evaluationAmount = (assetItem.evaluationAmount / exchangeRate).roundTo2Decimal(),
                evaluationProfitLoss = (assetItem.evaluationProfitLoss / exchangeRate).roundTo2Decimal(),
                creditLoanAmount = (assetItem.creditLoanAmount / exchangeRate).roundTo2Decimal(),
                realNetAssetAmount = (assetItem.realNetAssetAmount / exchangeRate).roundTo2Decimal(),
                wholeWeightRate = assetItem.wholeWeightRate
            )
        }

        return TotalAssetResponse(
            assets = assets,
            exchangeRate = exchangeRate,
            holdingStocks = accountStatus.holdings.map { stock ->
                val capital = capitalList.find { it.first == stock.ticker }?.second ?: 0.0
                HoldingStockMapper.toResponse(stock, capital)
            },
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