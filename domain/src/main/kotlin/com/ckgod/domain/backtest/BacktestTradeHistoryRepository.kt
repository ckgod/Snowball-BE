package com.ckgod.domain.backtest

import com.ckgod.domain.model.OrderStatus
import com.ckgod.domain.model.TradeHistory
import com.ckgod.domain.repository.TradeHistoryRepository
import java.time.LocalDateTime

class BacktestTradeHistoryRepository : TradeHistoryRepository {

    private val histories = mutableListOf<TradeHistory>()
    private var idSequence = 1L

    // 백테스트는 매일 미체결 주문을 찾고 주문마다 상태를 바꾼다. 매번 전체 목록을 훑으면
    // 주문 수의 제곱으로 느려져(10년치 2만 건에 30초) 주문번호 위치와 미체결 위치를 따로 들고 있는다
    private val indexByOrderNo = HashMap<String, Int>()
    private val pendingIndexes = sortedSetOf<Int>()

    override suspend fun save(history: TradeHistory): TradeHistory {
        val saved = history.copy(id = idSequence++)
        histories.add(saved)
        indexByOrderNo.putIfAbsent(saved.orderNo, histories.lastIndex)
        if (saved.status == OrderStatus.PENDING) pendingIndexes.add(histories.lastIndex)
        return saved
    }

    private fun replace(orderNo: String, transform: (TradeHistory) -> TradeHistory) {
        val index = indexByOrderNo[orderNo] ?: return
        val updated = transform(histories[index])
        histories[index] = updated
        if (updated.status == OrderStatus.PENDING) pendingIndexes.add(index) else pendingIndexes.remove(index)
    }

    override suspend fun updateOrderStatus(
        orderNo: String,
        status: OrderStatus,
        filledQuantity: Int,
        filledPrice: Double,
        filledTime: LocalDateTime,
        realizedProfitAmount: Double?
    ) {
        replace(orderNo) {
            it.copy(
                status = status,
                filledQuantity = filledQuantity,
                filledPrice = filledPrice,
                filledTime = filledTime,
                realizedProfitAmount = realizedProfitAmount ?: 0.0
            )
        }
    }

    override suspend fun findByOrderNo(orderNo: String): TradeHistory? {
        return indexByOrderNo[orderNo]?.let { histories[it] }
    }

    override suspend fun findByTicker(ticker: String, limit: Int): List<TradeHistory> {
        return histories.filter { it.ticker == ticker }.takeLast(limit)
    }

    override suspend fun findAll(limit: Int): List<TradeHistory> {
        return histories.takeLast(limit)
    }

    override suspend fun updateStatus(orderNo: String, status: OrderStatus) {
        replace(orderNo) { it.copy(status = status) }
    }

    override suspend fun markRejected(orderNo: String, reason: String) {
        replace(orderNo) { it.copy(status = OrderStatus.REJECTED, failReason = reason) }
    }

    override suspend fun findPendingOrders(): List<TradeHistory> {
        return pendingIndexes.map { histories[it] }
    }

    override suspend fun findByYesterdayOrderTime(ticker: String): List<TradeHistory> {
        return emptyList()
    }

    fun getAllHistories(): List<TradeHistory> = histories.toList()
}
