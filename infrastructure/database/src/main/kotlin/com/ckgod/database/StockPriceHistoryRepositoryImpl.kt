package com.ckgod.database

import com.ckgod.domain.model.StockPriceHistory
import com.ckgod.domain.repository.StockPriceHistoryRepository
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.toKotlinLocalDateTime
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * StockPriceHistory Repository 구현체
 *
 * Exposed ORM을 사용하여 MariaDB와 통신
 */
class StockPriceHistoryRepositoryImpl : StockPriceHistoryRepository {

    override suspend fun getPriceByDate(ticker: String, date: LocalDate): StockPriceHistory? = transaction {
        StockPriceHistoryTable.selectAll()
            .where {
                (StockPriceHistoryTable.ticker eq ticker) and
                (StockPriceHistoryTable.date eq date.toJavaLocalDate())
            }
            .singleOrNull()
            ?.toStockPriceHistory()
    }

    override suspend fun getPriceRange(
        ticker: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<StockPriceHistory> = transaction {
        StockPriceHistoryTable.selectAll()
            .where {
                (StockPriceHistoryTable.ticker eq ticker) and
                (StockPriceHistoryTable.date greaterEq startDate.toJavaLocalDate()) and
                (StockPriceHistoryTable.date lessEq endDate.toJavaLocalDate())
            }
            .orderBy(StockPriceHistoryTable.date to SortOrder.ASC)
            .map { it.toStockPriceHistory() }
    }

    override suspend fun getLatestPrice(ticker: String): StockPriceHistory? = transaction {
        StockPriceHistoryTable.selectAll()
            .where { StockPriceHistoryTable.ticker eq ticker }
            .orderBy(StockPriceHistoryTable.date to SortOrder.DESC)
            .limit(1)
            .singleOrNull()
            ?.toStockPriceHistory()
    }

    override suspend fun getAllPricesByDate(date: LocalDate): List<StockPriceHistory> = transaction {
        StockPriceHistoryTable.selectAll()
            .where { StockPriceHistoryTable.date eq date.toJavaLocalDate() }
            .orderBy(StockPriceHistoryTable.ticker to SortOrder.ASC)
            .map { it.toStockPriceHistory() }
    }

    override suspend fun getRecentPrices(ticker: String, days: Int): List<StockPriceHistory> = transaction {
        StockPriceHistoryTable.selectAll()
            .where { StockPriceHistoryTable.ticker eq ticker }
            .orderBy(StockPriceHistoryTable.date to SortOrder.DESC)
            .limit(days)
            .map { it.toStockPriceHistory() }
            .reversed()  // 오래된 것부터 최신 순으로 정렬
    }

    /**
     * ResultRow를 StockPriceHistory 도메인 모델로 변환
     */
    private fun ResultRow.toStockPriceHistory(): StockPriceHistory {
        return StockPriceHistory(
            id = this[StockPriceHistoryTable.id],
            ticker = this[StockPriceHistoryTable.ticker],
            date = this[StockPriceHistoryTable.date].toKotlinLocalDate(),
            open = this[StockPriceHistoryTable.open],
            high = this[StockPriceHistoryTable.high],
            low = this[StockPriceHistoryTable.low],
            close = this[StockPriceHistoryTable.close],
            adjClose = this[StockPriceHistoryTable.adjClose],
            volume = this[StockPriceHistoryTable.volume],
            createdAt = this[StockPriceHistoryTable.createdAt].toKotlinLocalDateTime(),
            updatedAt = this[StockPriceHistoryTable.updatedAt].toKotlinLocalDateTime()
        )
    }
}
