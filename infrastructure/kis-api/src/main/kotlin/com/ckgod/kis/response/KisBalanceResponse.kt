package com.ckgod.kis.response

import com.ckgod.domain.model.AccountStatus
import com.ckgod.domain.model.HoldingStock
import com.ckgod.kis.KisApiException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class KisBalanceResponse(
    @SerialName("rt_cd") val returnCode: String,
    @SerialName("msg_cd") val messageCode: String = "",
    @SerialName("msg1") val message: String,
    @SerialName("output1") val holdings: List<KisHoldingItem>? = null // 보유 종목 리스트
) {
    /**
     * 조회 실패면 예외를 던진다. 실패를 "보유 종목 없음"으로 바꾸면
     * 정산이 평단·누적 매수액을 0으로 덮어써 다음 주문이 첫 진입으로 나간다.
     */
    fun toDomain(): AccountStatus {
        if (returnCode != "0") {
            throw KisApiException("잔고 조회 실패 [$messageCode] $message")
        }
        val holdingList = holdings.orEmpty().map { item ->
            HoldingStock(
                ticker = item.ticker,
                name = item.itemName,
                quantity = item.quantity.kisNumber(),
                avgPrice = item.avgPrice.kisNumber(),
                investedAmount = item.purchaseAmt.kisNumber(),
                currentPrice = item.currentPrice.kisNumber(),
                profitRate = item.profitRate.kisNumber()
            )
        }

        return AccountStatus(
            holdings = holdingList
        )
    }
}

@Serializable
data class KisHoldingItem(
    @SerialName("ovrs_pdno") val ticker: String,           // 해외 상품 번호
    @SerialName("ovrs_item_name") val itemName: String,    // 종목명
    @SerialName("ovrs_cblc_qty") val quantity: String,     // 보유수량
    @SerialName("pchs_avg_pric") val avgPrice: String,     // 매입평균가격
    @SerialName("frcr_pchs_amt1") val purchaseAmt: String, // 외화매입금액
    @SerialName("ovrs_stck_evlu_amt") val evalAmt: String, // 외화평가금액
    @SerialName("frcr_evlu_pfls_amt") val profitLoss: String, // 외화평가손익금액
    @SerialName("evlu_pfls_rt") val profitRate: String,    // 수익률 (%)
    @SerialName("now_pric2") val currentPrice: String      // 현재가
)
