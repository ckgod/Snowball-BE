package com.ckgod.kis.response

import com.ckgod.domain.model.ExecutionInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KisExecutionResponse(
    @SerialName("rt_cd") val returnCode: String,
    @SerialName("msg_cd") val messageCode: String,
    @SerialName("msg1") val message: String,
    @SerialName("output") val output: List<ExecutionResponse>,
    @SerialName("ctx_area_fk200") val fKey: String,
    @SerialName("ctx_area_nk200") val nKey: String
) {
    @Serializable
    data class ExecutionResponse(
        @SerialName("ord_dt") val orderDate: String,
        @SerialName("odno") val orderNo: String,
        @SerialName("orgn_odno") val originOrderNo: String,
        @SerialName("sll_buy_dvsn_cd") val sellBuyCode: String,
        @SerialName("rvse_cncl_dvsn") val cancelCode: String,
        @SerialName("pdno") val ticker: String,
        @SerialName("prdt_name") val fullName: String,
        @SerialName("prcs_stat_name") val executionCode: String,
        @SerialName("ft_ord_qty") val orderQuantity: String,
        @SerialName("ft_ord_unpr3") val orderPrice: String,
        @SerialName("ft_ccld_qty") val executionQuantity: String,
        @SerialName("ft_ccld_unpr3") val executionPrice: String,
        @SerialName("ft_ccld_amt3") val executionAmount: String,
        @SerialName("nccs_qty") val notExecutionQuantity: String,
    ) {
        fun toDomain(): ExecutionInfo {
            return ExecutionInfo(
                ticker = ticker,
                orderNo = orderNo,
                orderQuantity = orderQuantity.toDouble(),
                filledPrice = executionPrice.toDouble(),
                filledQuantity = executionQuantity.toDouble()
            )
        }
    }
}
