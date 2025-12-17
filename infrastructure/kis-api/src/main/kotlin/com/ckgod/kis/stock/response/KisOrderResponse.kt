package com.ckgod.kis.stock.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KisOrderResponse(
    @SerialName("rt_cd") val returnCode: String,
    @SerialName("msg_cd") val messageCode: String,
    @SerialName("msg1") val message: String,
    @SerialName("output") val output: Output,
) {
    @Serializable
    data class Output(
        @SerialName("KRX_FWDG_ORD_ORGNO") val code: String,
        @SerialName("ODNO") val orderNo: String,
        @SerialName("ORD_TMD") val date: String,
    )
}
