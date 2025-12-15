package com.ckgod.database

import com.ckgod.domain.model.TradeHistory
import com.ckgod.domain.model.TradeType
import com.ckgod.domain.repository.TradeHistoryRepository
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * TradeHistory Repository 구현체
 */
class TradeHistoryRepositoryImpl : TradeHistoryRepository {

    override suspend fun save(history: TradeHistory): TradeHistory = transaction {
        val id = TradeHistoryTable.insert {
            it[date] = history.date
            it[type] = history.type.name
            it[price] = history.price
            it[quantity] = history.quantity
            it[profit] = history.profit
            it[tValueAt] = history.tValueAt
        } get TradeHistoryTable.id

        history.copy(id = id)
    }

    override suspend fun findAll(limit: Int): List<TradeHistory> = transaction {
        TradeHistoryTable.selectAll()
            .orderBy(TradeHistoryTable.id to org.jetbrains.exposed.v1.core.SortOrder.DESC)
            .limit(limit)
            .map { it.toTradeHistory() }
    }

    private fun org.jetbrains.exposed.v1.core.ResultRow.toTradeHistory(): TradeHistory {
        return TradeHistory(
            id = this[TradeHistoryTable.id],
            date = this[TradeHistoryTable.date],
            type = TradeType.valueOf(this[TradeHistoryTable.type]),
            price = this[TradeHistoryTable.price],
            quantity = this[TradeHistoryTable.quantity],
            profit = this[TradeHistoryTable.profit],
            tValueAt = this[TradeHistoryTable.tValueAt]
        )
    }
}
