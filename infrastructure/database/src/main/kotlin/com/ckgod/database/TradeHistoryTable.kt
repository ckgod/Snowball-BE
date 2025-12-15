package com.ckgod.database

import org.jetbrains.exposed.v1.core.Table

/**
 * trade_history 테이블
 *
 * 모든 거래 내역 저장
 */
object TradeHistoryTable : Table("trade_history") {
    val id = long("id").autoIncrement()                  // PK
    val date = varchar("date", 20)                       // 거래 일자
    val type = varchar("type", 20)                       // BUY / SELL / QUARTER_SELL
    val price = double("price")                          // 체결 가격
    val quantity = integer("quantity")                   // 체결 수량
    val profit = double("profit")                        // 실현 수익금
    val tValueAt = double("t_value_at")                  // 당시 T값

    override val primaryKey = PrimaryKey(id)
}
