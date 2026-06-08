package com.ckgod.kis.api

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * 미국 거래소 코드 매핑.
 *
 * 정규장(regular)과 주간거래(데이마켓, day)는 같은 종목이라도 EXCD가 다르다.
 * 주간거래는 한국 낮 시간대에 체결되는 세션으로, KIS 현재가상세 API(HHDFS76200200)에서
 * 주간 EXCD로 조회하면 해당 세션의 실시간 시세가 반환된다.
 *   - 정규장 NAS(나스닥) ↔ 주간 BAQ
 *   - 정규장 AMS(아멕스) ↔ 주간 BAA
 *   - 정규장 NYS(뉴욕)   ↔ 주간 BAY
 */
enum class UsExchange(val regular: String, val day: String) {
    NASDAQ("NAS", "BAQ"),
    AMEX("AMS", "BAA"),
    NYSE("NYS", "BAY");

    companion object {
        fun of(ticker: String): UsExchange = when (ticker) {
            "TQQQ" -> NASDAQ
            "SOXL", "FNGU", "SOXS" -> AMEX
            else -> NASDAQ
        }
    }
}

/**
 * 미국 주간거래(데이마켓, Blue Ocean ATS) 운영 시간 판별.
 *
 * 운영시간: 미국 동부시간(ET) 20:00 ~ 익일 04:00 (서머타임 자동 처리).
 * 이 창은 한국시간 기준 대략 09:00~17:00(EDT) / 10:00~18:00(EST)에 해당한다.
 *
 * 이 판별은 "데이터장을 먼저 시도할지" 결정하는 최적화일 뿐이며, 정확성은 호출부의
 * 빈 응답 폴백(데이터장 응답이 비면 정규장으로 재조회)이 보장한다. 따라서 주말·공휴일·
 * 세션 경계의 미세한 오차는 폴백이 자동 보정한다.
 */
object UsDayMarketSession {
    private val ET = ZoneId.of("America/New_York")
    private val OPEN = LocalTime.of(20, 0)
    private val CLOSE = LocalTime.of(4, 0)

    fun isLikelyOpen(now: Instant = Instant.now()): Boolean {
        val t = now.atZone(ET).toLocalTime()
        return t >= OPEN || t < CLOSE
    }
}
