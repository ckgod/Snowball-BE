package com.ckgod.kis.response

import com.ckgod.domain.model.AccountStatus
import com.ckgod.domain.model.HoldingStock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class KisBalanceResponse(
    @SerialName("rt_cd") val returnCode: String,
    @SerialName("msg1") val message: String,
    @SerialName("output1") val holdings: List<KisHoldingItem>?, // 보유 종목 리스트
    @SerialName("output2") val summary: KisAccountSummary?      // 계좌 전체 현황
) {
    fun toDomain(): AccountStatus {
        val holdingList = this.holdings?.map { item ->
            HoldingStock(
                ticker = item.ticker,
                name = item.itemName,
                quantity = item.quantity.ifEmpty { "0" },
                avgPrice = item.avgPrice.ifEmpty { "0.0" },
                investedAmount = item.purchaseAmt.ifEmpty { "0.0" },
                currentPrice = item.currentPrice.ifEmpty { "0.0" },
                profitRate = item.profitRate.ifEmpty { "0.0" }
            )
        } ?: emptyList()

        val summaryItem = this.summary

        val purchaseAmtStr = summaryItem?.totalPurchaseAmt ?: "0.0"
        val profitLossStr = summaryItem?.totalOverseasProfitLoss ?: "0.0"

        return AccountStatus(
            totalPurchaseAmount = purchaseAmtStr,
            totalEvaluationAmount = summaryItem?.totalEvalProfitLossAmt ?: "0", // 계산된 평가금액
            totalProfitOrLoss = profitLossStr,
            totalProfitRate = summaryItem?.totalProfitRate ?: "0.0",
            holdings = holdingList
        )
    }
}

@Serializable
data class KisHoldingItem(
    @SerialName("ovrs_pdno") val ticker: String,           // 해외 상품 번호
    @SerialName("ovrs_item_name") val itemName: String,    // 종목명
    @SerialName("ovrs_cblc_qty") val quantity: String,     // ★ 보유수량 (매도 시 1/4 계산용)
    @SerialName("pchs_avg_pric") val avgPrice: String,     // ★ 매입평균가격 (평단 LOC 기준)
    @SerialName("frcr_pchs_amt1") val purchaseAmt: String, // ★ 외화매입금액 (T값 계산용: 분자)
    @SerialName("ovrs_stck_evlu_amt") val evalAmt: String, // 외화평가금액
    @SerialName("frcr_evlu_pfls_amt") val profitLoss: String, // 외화평가손익금액
    @SerialName("evlu_pfls_rt") val profitRate: String,    // 수익률 (%)
    @SerialName("now_pric2") val currentPrice: String      // 현재가 (참고용)
)

@Serializable
data class KisAccountSummary(
    @SerialName("frcr_pchs_amt1") val totalPurchaseAmt: String,             // 외화매입금액1 (총 매수 원금)
    @SerialName("ovrs_rlzt_pfls_amt") val realizedProfitLoss: String,       // 해외실현손익금액 (매도 후 확정된 수익금)
    @SerialName("ovrs_tot_pfls") val totalOverseasProfitLoss: String,       // 해외총손익 (현재 보유중인 종목의 평가손익 합계)
    @SerialName("rlzt_erng_rt") val realizedReturnRate: String,             // 실현수익율 (매도 확정 수익률)
    @SerialName("tot_evlu_pfls_amt") val totalEvalProfitLossAmt: String,    // 총평가손익금액 (해외총손익과 유사하나 전체 자산 기준일 수 있음)
    @SerialName("tot_pftrt") val totalProfitRate: String                    // 총수익률 (전체 계좌 수익률)
)