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

    fun toDomain(): MarketPrice {
        val parsedTicker = rsym.substringAfter("@", rsym).ifEmpty { rsym }

        // 등락 상태 판단
        val priceStatus = when (changeSign) {
            "1" -> PriceStatus.UPPER_LIMIT
            "2" -> PriceStatus.UP
            "3" -> PriceStatus.FLAT
            "4" -> PriceStatus.DOWN
            "5" -> PriceStatus.LOWER_LIMIT
            else -> PriceStatus.UNKNOWN
        }

        return MarketPrice(
            ticker = parsedTicker,
            price = currentPrice,
            previousClose = previousClose,
            changeRate = krwChangeRate,
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
            status = priceStatus
        )
    }
}
