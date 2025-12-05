package com.ckgod.infrastructure.kis.api.spec

import com.ckgod.config.KisMode
import io.ktor.http.*

sealed class KisApiSpec(
    val method: HttpMethod,
    val path: String,
    private val realTrId: String,
    private val mockTrId: String,
    val description: String
) {
    fun getTrId(mode: KisMode): String = when (mode) {
        KisMode.REAL -> realTrId
        KisMode.MOCK -> mockTrId
    }

    /**
     * 주식 현재가 조회
     * GET /uapi/domestic-stock/v1/quotations/inquire-price
     */
    data object InquirePrice : KisApiSpec(
        method = HttpMethod.Get,
        path = "/uapi/domestic-stock/v1/quotations/inquire-price",
        realTrId = "FHKST01010100",
        mockTrId = "FHKST01010100",
        description = "주식 현재가 조회"
    ) {
        fun buildQuery(
            stockCode: String,
            marketDivCode: String = "J"
        ): Map<String, String> = mapOf(
            "FID_COND_MRKT_DIV_CODE" to marketDivCode,
            "FID_INPUT_ISCD" to stockCode
        )
    }
}
