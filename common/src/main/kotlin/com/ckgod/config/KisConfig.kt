package com.ckgod.config

data class KisConfig(
    val mode: KisMode,
    val baseUrl: String,
    val appKey: String,
    val appSecret: String,
    val accountNo: String
)
