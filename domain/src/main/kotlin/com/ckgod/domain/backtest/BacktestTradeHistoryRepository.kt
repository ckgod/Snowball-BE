package com.ckgod.domain.backtest

import com.ckgod.domain.model.OrderStatus
import com.ckgod.domain.model.TradeHistory
import com.ckgod.domain.repository.TradeHistoryRepository
import java.time.LocalDateTime

class BacktestTradeHistoryRepository : TradeHistoryRepository {

    private val histories = mutableListOf<TradeHistory>()
    private var idSequence = 1L

    override suspend fun save(history: TradeHistory): TradeHistory {
        val saved = history.copy(id = idSequence++)
        histories.add(saved)
        return saved
    }

    override suspend fun updateOrderStatus(
        orderNo: String,
        status: OrderStatus,
        filledQuantity: Int,
        filledPrice: Double,
        filledTime: LocalDateTime,
        realizedProfitAmount: Double?
    ) {
        val index = histories.indexOfFirst { it.orderNo == orderNo }
        if (index >= 0) {
            histories[index] = histories[index].copy(
                status = status,
                filledQuantity = filledQuantity,
                filledPrice = filledPrice,
                filledTime = filledTime,
                realizedProfitAmount = realizedProfitAmount ?: 0.0
            )
        }
    }

    override suspend fun findByOrderNo(orderNo: String): TradeHistory? {
        return histories.find { it.orderNo == orderNo }
    }

    override suspend fun findByTicker(ticker: String, limit: Int): List<TradeHistory> {
        return histories.filter { it.ticker == ticker }.takeLast(limit)
    }

    override suspend fun findAll(limit: Int): List<TradeHistory> {
        return histories.takeLast(limit)
    }

    override suspend fun findPendingOrders(): List<TradeHistory> {
        return histories.filter { it.status == OrderStatus.PENDING }
    }

    override suspend fun findByYesterdayOrderTime(ticker: String): List<TradeHistory> {
        return emptyList()
    }

    fun getAllHistories(): List<TradeHistory> = histories.toList()
}
