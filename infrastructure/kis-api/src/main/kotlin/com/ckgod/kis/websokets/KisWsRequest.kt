package com.ckgod.kis.websokets

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class KisWsRequest(
    val header: Header,
    val body: Body
) {
    @Serializable
    data class Header(
        val approval_key: String,
        val custtype: String = "P",
        val tr_type: String = "1",
        val `content-type`: String = "utf-8",
    )

    @Serializable
    data class Body(
        val input: Input
    ) {
        constructor(tr_key: String) : this(Input(tr_key = tr_key))
    }

    @Serializable
    data class Input(
        @SerialName("tr_id") val tr_id: String = "H0GSCNI0",
        @SerialName("tr_key") val tr_key: String
    )
}

@Serializable
data class KisWsResponse(
    val header: Header,
    val body: Body
) {
    @Serializable
    data class Header(
        val tr_id: String,
        val tr_key: String,
        val encrypt: String
    )

    @Serializable
    data class Body(
        val rt_cd: String,
        val msg_cd: String,
        val msg1: String,
        val output: Output? = null
    )

    @Serializable
    data class Output(
        val iv: String,
        val key: String
    )
}

data class RealtimeExecutionNotification(
    val customerId: String,
    val accountNumber: String,
    val orderNumber: String,
    val originalOrderNumber: String,
    val sellBuyType: String,
    val correctionType: String,
    val orderType2: String,
    val stockShortCode: String,
    val executionQuantity: String,
    val executionPrice: String,
    val stockExecutionTime: String,
    val rejectionFlag: String,
    val executionFlag: String,
    val acceptanceFlag: String,
    val branchNumber: String,
    val orderQuantity: String,
    val accountName: String,
    val executionStockName: String,
    val overseasStockType: String,
    val collateralTypeCode: String,
    val collateralLoanDate: String,
    val splitBuySellStartTime: String,
    val splitBuySellEndTime: String,
    val timeSplitTypeCode: String
) {
    companion object {
        fun from(fields: List<String>): RealtimeExecutionNotification {
            require(fields.size >= 24) { "필드 개수가 부족합니다. 필요: 24개, 실제: ${fields.size}개" }

            return RealtimeExecutionNotification(
                customerId = fields[0],
                accountNumber = fields[1],
                orderNumber = fields[2],
                originalOrderNumber = fields[3],
                sellBuyType = fields[4],
                correctionType = fields[5],
                orderType2 = fields[6],
                stockShortCode = fields[7],
                executionQuantity = fields[8],
                executionPrice = fields[9],
                stockExecutionTime = fields[10],
                rejectionFlag = fields[11],
                executionFlag = fields[12],
                acceptanceFlag = fields[13],
                branchNumber = fields[14],
                orderQuantity = fields[15],
                accountName = fields[16],
                executionStockName = fields[17],
                overseasStockType = fields[18],
                collateralTypeCode = fields[19],
                collateralLoanDate = fields[20],
                splitBuySellStartTime = fields[21],
                splitBuySellEndTime = fields[22],
                timeSplitTypeCode = fields[23]
            )
        }
    }
}

