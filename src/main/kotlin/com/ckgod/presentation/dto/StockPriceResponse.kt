package com.ckgod.presentation.dto

import kotlinx.serialization.Serializable

@Serializable
data class StockPriceResponse(
    val code: String,
    val name: String,
    val currentPrice: String,
    val changeRate: String,
    val volume: String
)

@Serializable
data class AccountBalanceResponse(
    val totalAsset: String,
    val cashBalance: String,
    val profitRate: String,
    val holdings: List<HoldingStock>
)

@Serializable
data class HoldingStock(
    val code: String,
    val name: String,
    val quantity: String,
    val profitRate: String
)
