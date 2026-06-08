package com.ckgod.kis.response

import com.ckgod.domain.model.MarketPrice
import com.ckgod.domain.model.PriceStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KisPriceResponse(
    @SerialName("rt_cd") val returnCode: String, // 성공 시 "0"
    @SerialName("msg1") val message: String,
    @SerialName("output") val output: KisPriceOutput?
)

@Serializable
data class KisPriceOutput(
    @SerialName("rsym") val rsym: String,        // 실시간 종목코드 (예: DNASTQQQ)
    @SerialName("curr") val currency: String,    // 통화 (USD)
    @SerialName("last") val currentPrice: String, // 현재가
    @SerialName("base") val previousClose: String, // 전일 종가
    @SerialName("open") val open: String,        // 시가
    @SerialName("high") val high: String,        // 고가
    @SerialName("low") val low: String,          // 저가
    @SerialName("tvol") val volume: String,      // 거래량
    @SerialName("tamt") val tradeAmount: String, // 거래대금
    @SerialName("t_xprc") val krwPrice: String,  // 원화환산 당일 가격
    @SerialName("t_xdif") val krwChangeAmount: String, // 원화환산 등락액
    @SerialName("t_xrat") val krwChangeRate: String,   // 원화환산 등락율
    @SerialName("t_xsgn") val changeSign: String,      // 등락 부호 (2:상승, 3:보합, 4:하락, 5:하한)
    @SerialName("t_rate") val exchangeRate: String,    // 환율
    @SerialName("h52p") val high52Week: String,  // 52주 최고가
    @SerialName("l52p") val low52Week: String,   // 52주 최저가
    @SerialName("etyp_nm") val productType: String // 상품 유형 (ETF 등)
) {

    /**
     * @param requestedTicker 조회 시 사용한 종목 코드. rsym은 세션에 따라 prefix가 달라지므로
     *   (정규장 DNASTQQQ ↔ 주간 RBAQTQQQ) 신뢰할 수 없어, 요청한 종목 코드를 그대로 사용한다.
     */
    fun toDomain(requestedTicker: String): MarketPrice {
        val last = currentPrice.toDoubleOrNull()
        val base = previousClose.toDoubleOrNull()

        // 등락율은 (현재가 - 전일종가) / 전일종가 로 직접 계산한다.
        // API의 t_xrat(원환산당일등락)은 환율 변동이 섞인 원화 기준이라 부정확하고,
        // 주간거래(데이마켓) 시 base는 정규장 종가이므로 이 계산이 "정규장 종가 대비 등락율"로 정확히 맞는다.
        val computedChangeRate = if (last != null && base != null && base != 0.0) {
            (last - base) / base * 100.0
        } else {
            null
        }

        // 등락 상태 판단 (미국 ETF는 상/하한가 개념이 없어 UP/DOWN/FLAT로 판단)
        val priceStatus = when {
            last == null || base == null -> PriceStatus.UNKNOWN
            last > base -> PriceStatus.UP
            last < base -> PriceStatus.DOWN
            else -> PriceStatus.FLAT
        }

        // rsym은 [prefix][3자리 거래소코드][심볼] 형식 (정규장 DAMSSOXL / 주간 RBAASOXL).
        // 거래소코드가 주간거래 코드(BAQ/BAA/BAY)면 데이마켓 시세다.
        // 폴백으로 정규장이 반환됐다면 rsym도 정규장 코드라 자연히 false가 된다.
        val isDayMarket = rsym.length >= 4 && rsym.substring(1, 4) in setOf("BAQ", "BAA", "BAY")

        return MarketPrice(
            ticker = requestedTicker,
            price = currentPrice,
            previousClose = previousClose,
            changeRate = computedChangeRate?.let { "%.2f".format(it) } ?: krwChangeRate,
            open = open,
            high = high,
            low = low,
            volume = volume,
            krwPrice = krwPrice,
            krwChangeAmount = krwChangeAmount,
            exchangeRate = exchangeRate,
            currency = currency,
            high52Week = high52Week,
            low52Week = low52Week,
            productType = productType,
            status = priceStatus,
            isDayMarket = isDayMarket
        )
    }
}
