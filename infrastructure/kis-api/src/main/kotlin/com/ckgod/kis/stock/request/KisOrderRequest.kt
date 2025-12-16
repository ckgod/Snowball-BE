package com.ckgod.kis.stock.request

import com.ckgod.domain.model.OrderRequest
import com.ckgod.kis.config.KisConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KisOrderRequest(
    @SerialName("CANO") val accountNo: String,
    @SerialName("ACNT_PRDT_CD") val accountCode: String,
    @SerialName("OVRS_EXCG_CD") val exchange: String,
    @SerialName("PDNO") val ticker: String,
    @SerialName("ORD_QTY") val quantity: String,
    @SerialName("OVRS_ORD_UNPR") val price: String,
    @SerialName("ORD_SVR_DVSN_CD") val severCode: String = "0",
    @SerialName("ORD_DVSN") val oderType: String,
) {
    companion object {
        fun from(config: KisConfig, orderRequest: OrderRequest) = KisOrderRequest(
            accountNo = config.accountNo,
            accountCode = config.accountCode,
            exchange = orderRequest.exchange.code,
            ticker = orderRequest.ticker,
            quantity = orderRequest.quantity.toString(),
            price = String.format("%.2f", orderRequest.price),
            oderType = orderRequest.type.code
        )
    }
}
