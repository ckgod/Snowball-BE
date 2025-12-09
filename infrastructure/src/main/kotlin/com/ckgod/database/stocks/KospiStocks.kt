package com.ckgod.database.stocks

import org.jetbrains.exposed.v1.core.Table

object KospiStocks : Table("kospi_stocks") {
    val shortCode = varchar("short_code", 20)
    val standardCode = varchar("standard_code", 30)
    val name = varchar("name", 100)

    val groupCode = varchar("group_code", 2)
    val marketCapScale = varchar("market_cap_scale", 1)
    val sectorLarge = varchar("sector_large", 4)


    val basePrice = long("base_price").nullable()
    val isTradingHalted = bool("is_trading_halted")
    val isManaged = bool("is_managed")

    val marketWarning = varchar("market_warning", 2)

    override val primaryKey = PrimaryKey(shortCode)
}