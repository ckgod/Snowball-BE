package com.ckgod.kis.response

import com.ckgod.domain.model.OpenOrder
import com.ckgod.domain.model.OrderSide
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 해외주식 미체결내역 응답 (TTTS3018R)
 */
@Serializable
data class KisOpenOrderResponse(
    @SerialName("rt_cd") val returnCode: String,
    @SerialName("msg_cd") val messageCode: String = "",
    @SerialName("msg1") val message: String = "",
    @SerialName("output") val output: List<Item> = emptyList(),
    @SerialName("ctx_area_fk200") val fKey: String = "",
    @SerialName("ctx_area_nk200") val nKey: String = ""
) {
    @Serializable
    data class Item(
        @SerialName("ord_dt") val orderDate: String = "",
        @SerialName("odno") val orderNo: String = "",
        @SerialName("orgn_odno") val originalOrderNo: String = "",
        @SerialName("pdno") val ticker: String = "",
        @SerialName("sll_buy_dvsn_cd") val sellBuyCode: String = "", // 01 매도, 02 매수
        @SerialName("ord_tmd") val orderTime: String = "",
        @SerialName("ft_ord_qty") val orderQuantity: String = "0",
        @SerialName("ft_ccld_qty") val filledQuantity: String = "0",
        @SerialName("nccs_qty") val unfilledQuantity: String = "0",
        @SerialName("ft_ord_unpr3") val orderPrice: String = "0",
        @SerialName("ovrs_excg_cd") val exchange: String = "",
    ) {
        fun toDomain() = OpenOrder(
            orderNo = orderNo,
            originalOrderNo = originalOrderNo.ifBlank { null },
            ticker = ticker,
            exchange = exchange,
            side = if (sellBuyCode == "01") OrderSide.SELL else OrderSide.BUY,
            orderPrice = orderPrice.toDoubleOrNull() ?: 0.0,
            orderQuantity = orderQuantity.toDoubleOrNull()?.toInt() ?: 0,
            filledQuantity = filledQuantity.toDoubleOrNull()?.toInt() ?: 0,
            unfilledQuantity = unfilledQuantity.toDoubleOrNull()?.toInt() ?: 0,
            orderDate = orderDate,
            orderTime = orderTime
        )
    }
}
