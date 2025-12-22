package com.ckgod.database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.datetime

/**
 * trade_history 테이블
 */
object TradeHistoryTable : Table("trade_history") {
    val id = long("id").autoIncrement()                     // PK
    val ticker = varchar("ticker", 20)                      // 종목명

    // 주문 정보
    val orderNo = varchar("order_no", 50).nullable()        // KIS 주문번호
    val orderSide = varchar("order_side", 20)               // BUY, SELL
    val orderType = varchar("order_type", 20)               // LIMIT, MOC, LOC
    val orderPrice = double("order_price")                  // 주문 가격
    val orderQuantity = integer("order_quantity")           // 주문 수량
    val orderTime = datetime("order_time")                  // 주문 시각

    // 체결 정보
    val status = varchar("status", 20).default("PENDING")   // PENDING, FILLED, PARTIAL, CANCELED
    val filledQuantity = integer("filled_quantity").default(0)     // 체결된 수량
    val filledPrice = double("filled_price").default(0.0)   // 체결 평균 가격
    val filledTime = datetime("filled_time").nullable()     // 체결 시각

    // 전략 정보
    val tValue = double("t_value")                          // 주문 당시 T값

    // 메타 정보
    val createdAt = datetime("created_at")                  // 생성 시각
    val updatedAt = datetime("updated_at")                  // 업데이트 시각

    override val primaryKey = PrimaryKey(id)
}
