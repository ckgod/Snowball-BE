package com.ckgod.kis.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KisOrderResponse(
    @SerialName("rt_cd") val returnCode: String,
    @SerialName("msg_cd") val messageCode: String = "",
    @SerialName("msg1") val message: String = "",
    // 거부 응답(rt_cd != "0")에는 output 이 오지 않는다
    @SerialName("output") val output: Output? = null,
) {
    val isSuccess: Boolean get() = returnCode == "0" && !output?.orderNo.isNullOrBlank()

    @Serializable
    data class Output(
        // 거부 응답에 빈 output 이 오는 경우까지 역직렬화되도록 기본값을 둔다
        @SerialName("KRX_FWDG_ORD_ORGNO") val code: String = "",
        @SerialName("ODNO") val orderNo: String = "",
        @SerialName("ORD_TMD") val date: String = "",
    )
}
