package com.ckgod.database

import org.jetbrains.exposed.v1.core.Table

/**
 * investment_status 테이블
 *
 * 단일 행만 존재 (ticker 기준)
 */
object InvestmentStatusTable : Table("investment_status") {
    val ticker = varchar("ticker", 20)                   // 종목명
    val currentT = double("current_t")                   // 현재 T값
    val totalInvested = double("total_invested")         // 매수 누적액
    val oneTimeAmount = double("one_time_amount")        // 1회 매수금
    val avgPrice = double("avg_price")                   // 평단가
    val targetRate = double("target_rate")               // 별%
    val buyLocPrice = double("buy_loc_price")            // 매수 주문 가격
    val sellLocPrice = double("sell_loc_price")          // 매도 주문 가격
    val updatedAt = varchar("updated_at", 50)            // 갱신 시간

    override val primaryKey = PrimaryKey(ticker)
}
