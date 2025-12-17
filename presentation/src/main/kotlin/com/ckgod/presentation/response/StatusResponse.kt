package com.ckgod.presentation.response

import kotlinx.serialization.Serializable

@Serializable
data class StatusResponse(
    val ticker: String,
    val currentT: Double,
    val targetRate: Double,
    val avgPrice: Double,
    val buyLocPrice: Double,
    val sellLocPrice: Double,
    val oneTimeAmount: Double,
    val totalInvested: Double,
    val updatedAt: String
)

@Serializable
data class StatusListResponse(
    val total: Int,
    val statusList: List<StatusResponse>
)