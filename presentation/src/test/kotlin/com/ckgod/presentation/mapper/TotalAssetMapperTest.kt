package com.ckgod.presentation.mapper

import com.ckgod.domain.model.AssetItem
import com.ckgod.domain.model.PresentAccountStatus
import com.ckgod.domain.model.TotalAsset
import com.ckgod.snowball.model.AssetType
import kotlin.test.Test
import kotlin.test.assertEquals
import com.ckgod.domain.model.AssetType as DomainAssetType

class TotalAssetMapperTest {

    private fun row(type: DomainAssetType, purchase: Double = 0.0, eval: Double, pl: Double = 0.0) =
        AssetItem(type, purchase, eval, pl, creditLoanAmount = 0.0, realNetAssetAmount = eval, wholeWeightRate = 0.0)

    // 결제 기준 자산현황: 결제 전 매수 2주($100)가 주식에서 빠지고 외화 현금에 남아 있다 (환율 1000)
    private val settled = TotalAsset(
        assets = DomainAssetType.entries.map { type ->
            when (type) {
                DomainAssetType.RP_PROMISSORY_NOTE -> row(type, purchase = 200_000.0, eval = 201_000.0, pl = 1_000.0)
                DomainAssetType.OVERSEAS_STOCKS -> row(type, purchase = 1_050_000.0, eval = 900_000.0, pl = -150_000.0)
                DomainAssetType.FOREIGN_CURRENCY -> row(type, eval = 600_000.0)
                DomainAssetType.TOTAL -> row(type, purchase = 1_250_000.0, eval = 1_701_000.0, pl = -149_000.0)
                else -> row(type, eval = 0.0)
            }
        }
    )

    private val executed = PresentAccountStatus(
        totalAssetValueUsd = 1500.0,
        totalBuyingValueUsd = 1100.0,
        totalEvalValueUsd = 1000.0,
        totalProfitUsd = -100.0,
        totalProfitRate = -9.09,
        totalCashUsd = 600.0,
        orderableCashUsd = 500.0,
        lockedCashUsd = 100.0,
        holdings = emptyList()
    )

    @Test
    fun `해외주식과 외화는 체결 기준 달러 값으로 바꾸고 합계를 다시 낸다`() {
        val assets = TotalAssetMapper.toResponse(executed, settled, emptyList(), exchangeRate = 1000.0)
            .assets.associateBy { it.type }

        val stocks = assets.getValue(AssetType.OVERSEAS_STOCKS)
        assertEquals(1000.0, stocks.evaluationAmount)
        assertEquals(1100.0, stocks.purchaseAmount)
        assertEquals(-100.0, stocks.evaluationProfitLoss)

        assertEquals(500.0, assets.getValue(AssetType.FOREIGN_CURRENCY).evaluationAmount)
        assertEquals(201.0, assets.getValue(AssetType.RP_PROMISSORY_NOTE).evaluationAmount)

        val total = assets.getValue(AssetType.TOTAL)
        assertEquals(1701.0, total.realNetAssetAmount)
        assertEquals(1300.0, total.purchaseAmount)
        assertEquals(-99.0, total.evaluationProfitLoss)
        assertEquals(58.79, stocks.wholeWeightRate)
    }

    @Test
    fun `자산현황이 비어 있으면 빈 목록을 돌려준다`() {
        val response = TotalAssetMapper.toResponse(executed, TotalAsset(emptyList()), emptyList(), exchangeRate = 1000.0)
        assertEquals(emptyList(), response.assets)
    }
}
