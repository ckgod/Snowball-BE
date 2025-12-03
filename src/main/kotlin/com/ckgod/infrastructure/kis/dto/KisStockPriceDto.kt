package com.ckgod.infrastructure.kis.dto

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

data class KisStockPriceDto(
    val stckPrpr: String,      // 주식 현재가 (stck_prpr)
    val prdyCtrt: String,      // 전일 대비율 (prdy_ctrt)
    val acmlVol: String,       // 누적 거래량 (acml_vol)
    val prdyVrss: String,      // 전일 대비 (prdy_vrss)
    val prdyVrssSign: String   // 전일 대비 부호 (prdy_vrss_sign)
) {
    companion object {
        fun from(json: JsonObject): KisStockPriceDto {
            return KisStockPriceDto(
                stckPrpr = json["stck_prpr"]?.jsonPrimitive?.content ?: "0",
                prdyCtrt = json["prdy_ctrt"]?.jsonPrimitive?.content ?: "0.0",
                acmlVol = json["acml_vol"]?.jsonPrimitive?.content ?: "0",
                prdyVrss = json["prdy_vrss"]?.jsonPrimitive?.content ?: "0",
                prdyVrssSign = json["prdy_vrss_sign"]?.jsonPrimitive?.content ?: "0"
            )
        }
    }
}
