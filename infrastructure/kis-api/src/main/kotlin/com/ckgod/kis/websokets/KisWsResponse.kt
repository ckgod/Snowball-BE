package com.ckgod.kis.websokets

import kotlinx.serialization.Serializable


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
