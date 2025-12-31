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
        constructor(tr_id: String, tr_key: String) : this(Input(tr_id = tr_id, tr_key = tr_key))
    }

    @Serializable
    data class Input(
        @SerialName("tr_id") val tr_id: String = "H0GSCNI0",
        @SerialName("tr_key") val tr_key: String
    )
}

