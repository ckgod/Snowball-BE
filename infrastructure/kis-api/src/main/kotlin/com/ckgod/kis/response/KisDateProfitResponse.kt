package com.ckgod.kis.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KisDateProfitResponse(
    @SerialName("rt_cd") val returnCode: String,
    @SerialName("msg1") val message: String,
    @SerialName("output1") val details: List<KisProfitDetail>?,
)

@Serializable
data class KisProfitDetail(
    @SerialName("trad_day") val tradeDay: String,
    @SerialName("ovrs_pdno") val ticker: String,
    @SerialName("ovrs_item_name") val itemName: String,
    @SerialName("ovrs_rlzt_pfls_amt") val realizedProfitAmount: String,
    @SerialName("slcl_qty") val sellQuantity: String,
    @SerialName("pchs_avg_pric") val avgBuyPrice: String,
    @SerialName("avg_sll_unpr") val avgSellPrice: String,
    @SerialName("pftrt") val profitRate: String
)
