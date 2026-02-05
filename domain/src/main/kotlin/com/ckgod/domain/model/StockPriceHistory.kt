package com.ckgod.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * 주식 가격 히스토리 도메인 모델
 *
 * Yahoo Finance에서 수집한 일별 OHLCV 데이터
 * 백테스팅 및 과거 데이터 분석에 사용
 */
@Serializable
data class StockPriceHistory(
    val id: Long = 0L,
    val ticker: String,                  // 종목 심볼 (TQQQ, SOXL 등)
    val date: LocalDate,                 // 거래일

    // OHLCV 데이터
    val open: Double,                    // 시가
    val high: Double,                    // 고가
    val low: Double,                     // 저가
    val close: Double,                   // 종가
    val adjClose: Double,                // 조정 종가 (배당/분할 반영)
    val volume: Long,                    // 거래량

    val createdAt: LocalDateTime,        // 생성 시각
    val updatedAt: LocalDateTime         // 업데이트 시각
)
