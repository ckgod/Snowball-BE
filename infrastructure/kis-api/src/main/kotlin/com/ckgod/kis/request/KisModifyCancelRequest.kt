package com.ckgod.kis.request

import com.ckgod.kis.config.KisConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KisModifyCancelRequest(
    @SerialName("CANO") val accountNo: String,
    @SerialName("ACNT_PRDT_CD") val accountCode: String,
    @SerialName("OVRS_EXCG_CD") val exchange: String,
    @SerialName("PDNO") val ticker: String,
    @SerialName("ORGN_ODNO") val originalOrderNo: String,
    @SerialName("RVSE_CNCL_DVSN_CD") val modifyCancelCode: String,
    @SerialName("ORD_QTY") val quantity: String,
    @SerialName("OVRS_ORD_UNPR") val price: String,
    @SerialName("MGCO_APTM_ODNO") val managerOrderNo: String = "",
    @SerialName("ORD_SVR_DVSN_CD") val serverCode: String = "0",
) {
    companion object {
        private const val MODIFY = "01"
        private const val CANCEL = "02"

        fun modify(config: KisConfig, exchange: String, ticker: String, orderNo: String, quantity: Int, price: Double) =
            KisModifyCancelRequest(
                accountNo = config.accountNo,
                accountCode = config.accountCode,
                exchange = exchange,
                ticker = ticker,
                originalOrderNo = orderNo,
                modifyCancelCode = MODIFY,
                quantity = quantity.toString(),
                price = String.format("%.2f", price)
            )

        // 취소는 단가를 "0" 으로 보낸다 (KIS 명세)
        fun cancel(config: KisConfig, exchange: String, ticker: String, orderNo: String, quantity: Int) =
            KisModifyCancelRequest(
                accountNo = config.accountNo,
                accountCode = config.accountCode,
                exchange = exchange,
                ticker = ticker,
                originalOrderNo = orderNo,
                modifyCancelCode = CANCEL,
                quantity = quantity.toString(),
                price = "0"
            )
    }
}
