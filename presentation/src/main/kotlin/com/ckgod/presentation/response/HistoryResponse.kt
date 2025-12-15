package com.ckgod.presentation.response

import kotlinx.serialization.Serializable

@Serializable
data class HistoryResponse(
    val total: Int,
    val histories: List<HistoryItem>
)

@Serializable
data class HistoryItem(
    val id: Long,
    val date: String,
    val type: String,
    val price: Double,
    val quantity: Int,
    val profit: Double,
    val tValueAt: Double
)