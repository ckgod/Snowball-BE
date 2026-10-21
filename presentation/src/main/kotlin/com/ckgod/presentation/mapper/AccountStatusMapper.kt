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
        val assets = executedBasis(
            assets = totalAsset.assets.map { item ->
                AssetItemResponse(
                    type = AssetType.valueOf(item.type.name),
                    purchaseAmount = item.purchaseAmount / exchangeRate,
                    evaluationAmount = item.evaluationAmount / exchangeRate,
                    evaluationProfitLoss = item.evaluationProfitLoss / exchangeRate,
                    creditLoanAmount = item.creditLoanAmount / exchangeRate,
                    realNetAssetAmount = item.realNetAssetAmount / exchangeRate,
                    wholeWeightRate = item.wholeWeightRate
                )
            },
            accountStatus = accountStatus
        ).map { it.rounded() }

        return TotalAssetResponse(
            assets = assets,
            exchangeRate = exchangeRate,
            holdingStocks = accountStatus.holdings.map { stock ->
                val capital = capitalList.find { it.first == stock.ticker }?.second ?: 0.0
                HoldingStockMapper.toResponse(stock, capital)
            },
        )
    }

    /**
     * 보유 종목을 주어진 시세로 다시 평가한다. 시세가 없는 종목은 잔고 API 의 현재가를 그대로 쓴다.
     */
    internal fun repriced(status: PresentAccountStatus, prices: Map<String, Double>): PresentAccountStatus {
        if (prices.isEmpty()) return status
        val holdings = status.holdings.map { holding ->
            val price = prices[holding.ticker] ?: return@map holding
            val avg = holding.avgPrice.toDoubleOrNull() ?: 0.0
            holding.copy(
                currentPrice = price.toString(),
                profitRate = if (avg > 0) ((price - avg) / avg * 100).roundTo2Decimal().toString() else holding.profitRate
            )
        }
        val evaluation = holdings.sumOf {
            (it.quantity.toDoubleOrNull() ?: 0.0) * (it.currentPrice.toDoubleOrNull() ?: 0.0)
        }
        val profit = evaluation - status.totalBuyingValueUsd
        return status.copy(
            totalAssetValueUsd = (status.orderableCashUsd + evaluation).roundTo2Decimal(),
            totalEvalValueUsd = evaluation.roundTo2Decimal(),
            totalProfitUsd = profit.roundTo2Decimal(),
            totalProfitRate = if (status.totalBuyingValueUsd > 0) (profit / status.totalBuyingValueUsd * 100).roundTo2Decimal() else 0.0,
            holdings = holdings
        )
    }

    /**
     * 자산현황(CTRP6548R)은 결제 기준이라 체결됐지만 결제 전(T+1)인 매수분이 해외주식에서 빠지고 외화 현금에 남는다.
     * 봇이 매일 사므로 늘 어긋난다. 해외주식·외화 칸은 체결 기준 잔고(CTRP6504R)의 달러 값으로 바꾸고 합계와 비중을 다시 낸다.
     * 원화로만 오는 나머지 칸(RP 등)은 그대로 둔다.
     */
    internal fun executedBasis(
        assets: List<AssetItemResponse>,
        accountStatus: PresentAccountStatus,
    ): List<AssetItemResponse> {
        val replaced = assets.map { item ->
            when (item.type) {
                AssetType.OVERSEAS_STOCKS -> item.copy(
                    purchaseAmount = accountStatus.totalBuyingValueUsd,
                    evaluationAmount = accountStatus.totalEvalValueUsd,
                    evaluationProfitLoss = accountStatus.totalProfitUsd,
                    realNetAssetAmount = accountStatus.totalEvalValueUsd,
                )
                AssetType.FOREIGN_CURRENCY -> item.copy(
                    evaluationAmount = accountStatus.orderableCashUsd,
                    realNetAssetAmount = accountStatus.orderableCashUsd,
                )
                else -> item
            }
        }
        val parts = replaced.filter { it.type != AssetType.TOTAL }
        val total = parts.sumOf { it.realNetAssetAmount }
        return replaced.map { item ->
            if (item.type == AssetType.TOTAL) {
                item.copy(
                    purchaseAmount = parts.sumOf { it.purchaseAmount },
                    evaluationAmount = parts.sumOf { it.evaluationAmount },
                    evaluationProfitLoss = parts.sumOf { it.evaluationProfitLoss },
                    creditLoanAmount = parts.sumOf { it.creditLoanAmount },
                    realNetAssetAmount = total,
                    wholeWeightRate = 100.0,
                )
            } else {
                item.copy(wholeWeightRate = if (total > 0) item.realNetAssetAmount / total * 100 else 0.0)
            }
        }
    }

    private fun AssetItemResponse.rounded() = copy(
        purchaseAmount = purchaseAmount.roundTo2Decimal(),
        evaluationAmount = evaluationAmount.roundTo2Decimal(),
        evaluationProfitLoss = evaluationProfitLoss.roundTo2Decimal(),
        creditLoanAmount = creditLoanAmount.roundTo2Decimal(),
        realNetAssetAmount = realNetAssetAmount.roundTo2Decimal(),
        wholeWeightRate = wholeWeightRate.roundTo2Decimal(),
    )
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