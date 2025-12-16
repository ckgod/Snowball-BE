package com.ckgod.kis.stock.repository

import com.ckgod.domain.model.MarketPrice
import com.ckgod.domain.model.OrderRequest
import com.ckgod.domain.repository.StockRepository
import com.ckgod.kis.stock.api.KisApiService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class StockRepositoryImpl(private val kisApiService: KisApiService) : StockRepository {

    override suspend fun getCurrentPrice(stockCode: String): MarketPrice? {
        val kisData = kisApiService.getMarketCurrentPrice(
            stockCode= stockCode
        )

        return kisData.output?.toDomain()
    }

    override suspend fun postOrder(buyOrders: List<OrderRequest>, sellOrders: List<OrderRequest>) {
        coroutineScope {
            val sellJobs = sellOrders.map { order ->
                async { kisApiService.postOrder(order) }
            }

            val buyJobs = buyOrders.map { order ->
                async { kisApiService.postOrder(order) }
            }

            sellJobs.awaitAll()
            buyJobs.awaitAll()
        }
    }
}