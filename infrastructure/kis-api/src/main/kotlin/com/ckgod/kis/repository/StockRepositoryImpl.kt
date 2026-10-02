package com.ckgod.kis.repository

import com.ckgod.domain.model.MarketPrice
import com.ckgod.domain.model.OrderOutcome
import com.ckgod.domain.model.OrderRequest
import com.ckgod.domain.model.OrderRejection
import com.ckgod.domain.model.OrderResponse
import com.ckgod.domain.model.OrderSide
import com.ckgod.domain.model.OrderSubmission
import com.ckgod.domain.repository.StockRepository
import com.ckgod.kis.KisOrderRejectedException
import com.ckgod.kis.api.KisApiService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory

class StockRepositoryImpl(private val kisApiService: KisApiService) : StockRepository {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun getCurrentPrice(stockCode: String, includeDayMarket: Boolean): MarketPrice? {
        val kisData = kisApiService.getMarketCurrentPrice(
            stockCode = stockCode,
            includeDayMarket = includeDayMarket
        )

        return kisData.output?.toDomain(stockCode)
    }

    override suspend fun getExchangeRate(): Double {
        return kisApiService.getMarketCurrentPrice("TQQQ").output?.exchangeRate?.toDoubleOrNull() ?: 1450.0
    }

    override suspend fun postOrder(buyOrders: List<OrderRequest>, sellOrders: List<OrderRequest>): OrderSubmission {
        return coroutineScope {
            // 매도를 먼저 보내고 매수를 보낸다 (기존 순서 유지)
            val sellResults = sellOrders.map { order -> async { submit(order) } }.awaitAll()
            val buyResults = buyOrders.map { order -> async { submit(order) } }.awaitAll()
            val results = sellResults + buyResults

            OrderSubmission(
                accepted = results.filterIsInstance<OrderResponse>(),
                rejected = results.filterIsInstance<OrderRejection>()
            )
        }
    }

    private suspend fun submit(order: OrderRequest): OrderOutcome {
        val side = if (order.side == OrderSide.BUY) "매수" else "매도"
        return try {
            val output = requireNotNull(kisApiService.postOrder(order).output)
            OrderResponse(request = order, orderNo = output.orderNo, orderTime = output.date)
        } catch (e: KisOrderRejectedException) {
            logger.error("[${order.ticker}] $side 주문 거부: $order - ${e.message}")
            OrderRejection(request = order, reason = e.message ?: "KIS 주문 거부")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 응답을 못 받은 경우 KIS 쪽에서는 접수됐을 수도 있다. 사유에 확인 필요를 남긴다.
            logger.error("[${order.ticker}] $side 주문 실패: $order", e)
            OrderRejection(
                request = order,
                reason = "전송 오류(실제 접수 여부 확인 필요): ${e.message ?: e::class.simpleName}"
            )
        }
    }
}