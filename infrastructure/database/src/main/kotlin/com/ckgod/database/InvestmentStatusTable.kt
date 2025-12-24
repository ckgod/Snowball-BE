package com.ckgod.database

import org.jetbrains.exposed.v1.core.Table

/**
 * investment_status 테이블
 *
 * 단일 행만 존재 (ticker 기준)
 */
object InvestmentStatusTable : Table("investment_status") {
    val ticker = varchar("ticker", 20)                        // 종목명
    val fullName = varchar("full_name", 100).nullable()       // 종목 풀네임
    val totalInvested = double("total_invested")                     // 매수 누적액
    val oneTimeAmount = double("one_time_amount")                    // 1회 매수금
    val initialCapital = double("initial_capital")                   // 원금
    val division = integer("division").default(40)       // 분할 수
    val avgPrice = double("avg_price")                               // 평단가
    val quantity = integer("quantity").default(0)        // 보유 수량
    val targetRate = double("target_rate")                           // 기준 %
    val updatedAt = varchar("updated_at", 50)                 // 갱신 시간
    val realizedTotalProfit = double("realized_total_profit")        // 총 실현 손익

    override val primaryKey = PrimaryKey(ticker)
}
