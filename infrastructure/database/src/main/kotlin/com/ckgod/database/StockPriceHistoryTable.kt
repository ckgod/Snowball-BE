package com.ckgod.database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.datetime

/**
 * stock_price_history 테이블
 *
 * 레버리지 ETF의 일별 가격 정보를 저장
 * Yahoo Finance에서 수집한 OHLCV 데이터 저장
 * 백테스팅 및 과거 데이터 분석에 사용
 */
object StockPriceHistoryTable : Table("stock_price_history") {
    val id = long("id").autoIncrement()                     // PK
    val ticker = varchar("ticker", 20)                      // 종목 심볼 (TQQQ, SOXL 등)
    val date = date("date")                                 // 거래일

    // OHLCV 데이터
    val open = double("open")                               // 시가
    val high = double("high")                               // 고가
    val low = double("low")                                 // 저가
    val close = double("close")                             // 종가
    val adjClose = double("adj_close")                      // 조정 종가 (배당/분할 반영)
    val volume = long("volume")                             // 거래량

    val createdAt = datetime("created_at")                  // 생성 시각
    val updatedAt = datetime("updated_at")                  // 업데이트 시각

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(ticker, date)  // 종목+날짜 유니크 제약
        index(false, ticker)       // 종목별 조회 인덱스
        index(false, date)         // 날짜별 조회 인덱스
    }
}
