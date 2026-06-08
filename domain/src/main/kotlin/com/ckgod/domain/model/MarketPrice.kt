package com.ckgod.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MarketPrice(
    val ticker: String,                  // 종목 코드 (예: TQQQ)
    val price: String,                   // 현재가 (달러)
    val previousClose: String,           // 전일 종가 (달러)
    val changeRate: String,              // 등락율 (%)
    val open: String,                    // 시가 (달러)
    val high: String,                    // 고가 (달러)
    val low: String,                     // 저가 (달러)
    val volume: String,                  // 거래량
    val krwPrice: String,                // 원화환산 가격
    val krwChangeAmount: String,         // 원화환산 등락액
    val exchangeRate: String,            // 환율
    val currency: String = "USD",        // 통화
    val high52Week: String = "",         // 52주 최고가
    val low52Week: String = "",          // 52주 최저가
    val productType: String = "",        // 상품 유형 (ETF 등)
    val status: PriceStatus = PriceStatus.UNKNOWN, // 상승/하락 상태
    val isDayMarket: Boolean = false     // 현재가가 주간거래(데이마켓) 시세 기준인지 여부
)

@Serializable
enum class PriceStatus {
    UPPER_LIMIT, // 상한
    UP,          // 상승
    FLAT,        // 보합
    DOWN,        // 하락
    LOWER_LIMIT, // 하한
    UNKNOWN
}