package com.ckgod.database.migrations

import com.ckgod.database.TradeHistoryTable
import com.ckgod.domain.model.OrderSide
import com.ckgod.domain.model.OrderStatus
import com.ckgod.domain.model.OrderType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * 과거 거래 내역 데이터 마이그레이션
 */
object HistoricalDataMigration {

    fun migrate() {
        println("Migrating historical trade data...")

        data class HistoricalTrade(
            val ticker: String,
            val orderNo: String,
            val orderSide: OrderSide,
            val orderType: OrderType,
            val orderPrice: Double,
            val orderQuantity: Int,
            val orderTime: String,
            val status: OrderStatus,
            val filledQuantity: Int,
            val filledPrice: Double,
            val filledTime: String?,
            val tValue: Double
        )

        val historicalTrades = listOf(
            // 20251216 데이터
            HistoricalTrade("FNGU", "0030431699", OrderSide.BUY, OrderType.LOC, 28.9, 1, "20251216", OrderStatus.FILLED, 1, 25.57, "20251217", 0.0),
            HistoricalTrade("FNGU", "0030432003", OrderSide.BUY, OrderType.LOC, 23.62, 1, "20251216", OrderStatus.CANCELED, 0, 0.0, null, 0.0),
            HistoricalTrade("FNGU", "0030432004", OrderSide.BUY, OrderType.LOC, 22.62, 1, "20251216", OrderStatus.CANCELED, 0, 0.0, null, 0.0),
            HistoricalTrade("FNGU", "0030432005", OrderSide.BUY, OrderType.LOC, 21.36, 1, "20251216", OrderStatus.CANCELED, 0, 0.0, null, 0.0),
            HistoricalTrade("FNGU", "0030432006", OrderSide.BUY, OrderType.LOC, 22.11, 1, "20251216", OrderStatus.CANCELED, 0, 0.0, null, 0.0),
            // 20251217 데이터
            HistoricalTrade("FNGU", "0030690474", OrderSide.SELL, OrderType.LIMIT, 29.41, 1, "20251217", OrderStatus.CANCELED, 0, 0.0, null, 0.43),
            HistoricalTrade("FNGU", "0030690477", OrderSide.BUY, OrderType.LOC, 23.47, 1, "20251217", OrderStatus.CANCELED, 0, 0.0, null, 0.43),
            HistoricalTrade("FNGU", "0030690478", OrderSide.BUY, OrderType.LOC, 22.17, 1, "20251217", OrderStatus.CANCELED, 0, 0.0, null, 0.43),
            HistoricalTrade("FNGU", "0030690479", OrderSide.BUY, OrderType.LOC, 24.78, 1, "20251217", OrderStatus.FILLED, 1, 24.15, "20251218", 0.43),
            HistoricalTrade("FNGU", "0030690480", OrderSide.BUY, OrderType.LOC, 29.4, 1, "20251217", OrderStatus.FILLED, 1, 24.15, "20251218", 0.43),
            HistoricalTrade("FNGU", "0030690481", OrderSide.BUY, OrderType.LOC, 25.57, 1, "20251217", OrderStatus.FILLED, 1, 24.15, "20251218", 0.43),
            // 20251218 데이터
            HistoricalTrade("FNGU", "0030953094", OrderSide.SELL, OrderType.LIMIT, 28.18, 3, "20251218", OrderStatus.CANCELED, 0, 0.0, null, 0.76),
            HistoricalTrade("FNGU", "0030953095", OrderSide.SELL, OrderType.LOC, 27.66, 1, "20251218", OrderStatus.CANCELED, 0, 0.0, null, 0.76),
            HistoricalTrade("FNGU", "0030953114", OrderSide.BUY, OrderType.LOC, 22.94, 1, "20251218", OrderStatus.CANCELED, 0, 0.0, null, 0.76),
            HistoricalTrade("FNGU", "0030953116", OrderSide.BUY, OrderType.LOC, 24.51, 1, "20251218", OrderStatus.CANCELED, 0, 0.0, null, 0.76),
            HistoricalTrade("FNGU", "0030953117", OrderSide.BUY, OrderType.LOC, 27.26, 1, "20251218", OrderStatus.FILLED, 1, 24.85, "20251219", 0.76),
            HistoricalTrade("FNGU", "0030953118", OrderSide.BUY, OrderType.LOC, 21.74, 1, "20251218", OrderStatus.CANCELED, 0, 0.0, null, 0.76),
            HistoricalTrade("FNGU", "0030953119", OrderSide.BUY, OrderType.LOC, 20.53, 1, "20251218", OrderStatus.CANCELED, 0, 0.0, null, 0.76)
        )

        transaction {
            var insertedCount = 0
            var skippedCount = 0

            historicalTrades.forEach { trade ->
                // orderNo가 이미 존재하는지 확인
                val exists = TradeHistoryTable.selectAll()
                    .where { TradeHistoryTable.orderNo eq trade.orderNo }
                    .singleOrNull() != null

                if (!exists) {
                    TradeHistoryTable.insert {
                        it[ticker] = trade.ticker
                        it[orderNo] = trade.orderNo
                        it[orderSide] = trade.orderSide.name
                        it[orderType] = trade.orderType.name
                        it[orderPrice] = trade.orderPrice
                        it[orderQuantity] = trade.orderQuantity
                        it[orderTime] = trade.orderTime
                        it[status] = trade.status.name
                        it[filledQuantity] = trade.filledQuantity
                        it[filledPrice] = trade.filledPrice
                        it[filledTime] = trade.filledTime ?: ""
                        it[tValue] = trade.tValue
                        it[createdAt] = "2025-12-19T00:00:00"
                        it[updatedAt] = "2025-12-19T00:00:00"
                    }
                    insertedCount++
                } else {
                    skippedCount++
                }
            }

            println("Historical trade data migration completed: $insertedCount inserted, $skippedCount skipped")
        }
    }
}
