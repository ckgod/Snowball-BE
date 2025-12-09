package com.ckgod.domain.stock

interface StockRepository {
    fun saveAll(stocks: List<Stock>)
    fun deleteAll(): Int
    fun isEmpty(): Boolean
}
