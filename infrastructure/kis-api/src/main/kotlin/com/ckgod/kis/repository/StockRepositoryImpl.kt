package com.ckgod.kis.repository

import com.ckgod.domain.model.MarketPrice
import com.ckgod.domain.model.OrderRequest
import com.ckgod.domain.model.OrderResponse
import com.ckgod.domain.repository.StockRepository
import com.ckgod.kis.api.KisApiService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory

class StockRepositoryImpl(private val kisApiService: KisApiService) : StockRepository {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun getCurrentPrice(stockCode: String): MarketPrice? {
        val kisData = kisApiService.getMarketCurrentPrice(
            stockCode= stockCode
        )

        return kisData.output?.toDomain()
    }

    override suspend fun postOrder(buyOrders: List<OrderRequest>, sellOrders: List<OrderRequest>): List<OrderResponse> {
        return coroutineScope {
            val sellResponses = sellOrders.map { order ->
                async {
                    try {
                        val response = kisApiService.postOrder(order)
                        OrderResponse(
                            request = order,
                            orderNo = response.output.orderNo,
                            orderTime = response.output.date
                        )
                    } catch (e: Exception) {
                        logger.error("[${order.ticker}] 매도 주문 실패: $order", e)
                        null
                    }
                }
            }.awaitAll().filterNotNull()

            val buyResponses = buyOrders.map { order ->
                async {
                    try {
                        val response = kisApiService.postOrder(order)
                        OrderResponse(
                            request = order,
                            orderNo = response.output.orderNo,
                            orderTime = response.output.date
                        )
                    } catch (e: Exception) {
                        logger.error("[${order.ticker}] 매수 주문 실패: $order", e)
                        null
                    }
                }
            }.awaitAll().filterNotNull()

            sellResponses + buyResponses
        }
    }
}