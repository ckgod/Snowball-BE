package com.ckgod.database

import com.ckgod.domain.model.OrderSide
import com.ckgod.domain.model.OrderStatus
import com.ckgod.domain.model.OrderType
import com.ckgod.domain.model.TradeHistory
import com.ckgod.domain.repository.TradeHistoryRepository
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.LocalDateTime

/**
 * TradeHistory Repository 구현체
 */
class TradeHistoryRepositoryImpl : TradeHistoryRepository {

    override suspend fun save(history: TradeHistory): TradeHistory = transaction {
        val id = TradeHistoryTable.insert {
            it[ticker] = history.ticker
            it[orderNo] = history.orderNo
            it[orderSide] = history.orderSide.name
            it[orderType] = history.orderType.name
            it[orderPrice] = history.orderPrice
            it[orderQuantity] = history.orderQuantity
            it[orderTime] = history.orderTime
            it[status] = history.status.name
            it[filledQuantity] = history.filledQuantity
            it[filledPrice] = history.filledPrice
            it[filledTime] = history.filledTime
            it[tValue] = history.tValue
            it[createdAt] = history.createdAt
            it[updatedAt] = history.updatedAt
        } get TradeHistoryTable.id

        history.copy(id = id)
    }

    override suspend fun updateOrderStatus(
        orderNo: String,
        status: OrderStatus,
        filledQuantity: Int,
        filledPrice: Double,
        filledTime: LocalDateTime
    ) {
        transaction {
            TradeHistoryTable.update({ TradeHistoryTable.orderNo eq orderNo }) {
                it[TradeHistoryTable.status] = status.name
                it[TradeHistoryTable.filledQuantity] = filledQuantity
                it[TradeHistoryTable.filledPrice] = filledPrice
                it[TradeHistoryTable.filledTime] = filledTime
                it[updatedAt] = LocalDateTime.now()
            }
        }
    }

    override suspend fun findByOrderNo(orderNo: String): TradeHistory? = transaction {
        TradeHistoryTable.selectAll()
            .where { TradeHistoryTable.orderNo eq orderNo }
            .singleOrNull()
            ?.toTradeHistory()
    }

    override suspend fun findByTicker(ticker: String, limit: Int): List<TradeHistory> = transaction {
        TradeHistoryTable.selectAll()
            .where { TradeHistoryTable.ticker eq ticker }
            .orderBy(TradeHistoryTable.orderTime to SortOrder.DESC)
            .limit(limit)
            .map { it.toTradeHistory() }
    }

    override suspend fun findAll(limit: Int): List<TradeHistory> = transaction {
        TradeHistoryTable.selectAll()
            .orderBy(TradeHistoryTable.orderTime to SortOrder.DESC)
            .limit(limit)
            .map { it.toTradeHistory() }
    }

    override suspend fun findPendingOrders(): List<TradeHistory> = transaction {
        TradeHistoryTable.selectAll()
            .where { TradeHistoryTable.status eq OrderStatus.PENDING.name }
            .orderBy(TradeHistoryTable.orderTime to SortOrder.DESC)
            .map { it.toTradeHistory() }
    }

    private fun org.jetbrains.exposed.v1.core.ResultRow.toTradeHistory(): TradeHistory {
        return TradeHistory(
            id = this[TradeHistoryTable.id],
            ticker = this[TradeHistoryTable.ticker],
            orderNo = this[TradeHistoryTable.orderNo],
            orderSide = OrderSide.valueOf(this[TradeHistoryTable.orderSide]),
            orderType = OrderType.valueOf(this[TradeHistoryTable.orderType]),
            orderPrice = this[TradeHistoryTable.orderPrice],
            orderQuantity = this[TradeHistoryTable.orderQuantity],
            orderTime = this[TradeHistoryTable.orderTime],
            status = OrderStatus.valueOf(this[TradeHistoryTable.status]),
            filledQuantity = this[TradeHistoryTable.filledQuantity],
            filledPrice = this[TradeHistoryTable.filledPrice],
            filledTime = this[TradeHistoryTable.filledTime],
            tValue = this[TradeHistoryTable.tValue],
            createdAt = this[TradeHistoryTable.createdAt],
            updatedAt = this[TradeHistoryTable.updatedAt]
        )
    }
}
