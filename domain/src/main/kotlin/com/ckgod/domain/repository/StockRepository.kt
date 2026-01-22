package com.ckgod.domain.repository

import com.ckgod.domain.model.MarketPrice
import com.ckgod.domain.model.OrderRequest
import com.ckgod.domain.model.OrderResponse

interface StockRepository {
    suspend fun getCurrentPrice(stockCode: String): MarketPrice?

    suspend fun getExchangeRate(): Double

    suspend fun postOrder(
        buyOrders: List<OrderRequest> = emptyList(),
        sellOrders: List<OrderRequest> = emptyList()
    ): List<OrderResponse>
}
