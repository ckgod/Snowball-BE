package com.ckgod.database.stocks

import com.ckgod.domain.stock.Stock
import com.ckgod.domain.stock.StockRepository
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class StockRepositoryImpl : StockRepository {
    override fun saveAll(stocks: List<Stock>) = transaction {
        stocks.forEach { stock ->
            KospiStocks.upsert {
                it[KospiStocks.shortCode] = stock.shortCode
                it[KospiStocks.standardCode] = stock.standardCode
                it[KospiStocks.name] = stock.name
                it[KospiStocks.groupCode] = stock.groupCode
                it[KospiStocks.marketCapScale] = stock.marketCapScale
                it[KospiStocks.sectorLarge] = stock.sectorLarge
                it[KospiStocks.basePrice] = stock.basePrice
                it[KospiStocks.isTradingHalted] = stock.isTradingHalted
                it[KospiStocks.isManaged] = stock.isManaged
                it[KospiStocks.marketWarning] = stock.marketWarning
            }
        }
    }

    override fun deleteAll() = transaction {
        KospiStocks.deleteAll()
    }

    override fun isEmpty(): Boolean = transaction {
        KospiStocks.selectAll().count() == 0L
    }
}

