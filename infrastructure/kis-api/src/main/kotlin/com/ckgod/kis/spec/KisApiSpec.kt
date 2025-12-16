package com.ckgod.kis.spec

import com.ckgod.kis.config.KisMode
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
     * @see <a href="https://apiportal.koreainvestment.com/apiservice-apiservice?/uapi/overseas-price/v1/quotations/price-detail">해외주식 현재가 상세</a>
     */
    data object QuotationPriceDetail : KisApiSpec(
        method = HttpMethod.Get,
        path = "/uapi/overseas-price/v1/quotations/price-detail",
        realTrId = "HHDFS76200200",
        mockTrId = "모의투자 미지원",
        description = "해외주식 현재가 상세"
    ) {
        fun buildQuery(
            userId: String,
            exchange: String,
            stockCode: String
        ): Map<String, String> = mapOf(
            "AUTH" to userId,
            "EXCD" to exchange,
            "SYMB" to stockCode
        )
    }

    data object InquireBalance : KisApiSpec(
        method = HttpMethod.Get,
        path = "/uapi/overseas-stock/v1/trading/inquire-balance",
        realTrId = "TTTS3012R",
        mockTrId = "VTTS3012R",
        description = "해외주식 잔고 조회"
    ) {
        fun buildQuery(
            accountNo: String,
            accountCode: String,
            exchange: String = "NASD",
            currency: String = "USD"
        ): Map<String, String> = mapOf(
            "CANO" to accountNo,
            "ACNT_PRDT_CD" to accountCode,
            "OVRS_EXCG_CD" to exchange,
            "TR_CRCY_CD" to currency,
            "CTX_AREA_FK200" to "",
            "CTX_AREA_NK200" to ""
        )
    }

    data object InquirePeriodProfit : KisApiSpec(
        method = HttpMethod.Get,
        path = "/uapi/overseas-stock/v1/trading/inquire-period-profit",
        realTrId = "TTTS3039R",
        mockTrId = "모의투자 미지원",
        description = "해외주식 기간손익"
    ) {
        fun buildQuery(
            accountNo: String,
            accountCode: String,
            startDate: String,
            endDate: String,
            overseaCurrency: String = "01"
        ): Map<String, String> = mapOf(
            "CANO" to accountNo,
            "ACNT_PRDT_CD" to accountCode,
            "INQR_STRT_DT" to startDate,
            "INQR_END_DT" to endDate,
            "WCRC_FRCR_DVSN_CD" to overseaCurrency, // 원화 외화 구분 코드
            "OVRS_EXCG_CD" to "NASD", // 해외 거래소 코드
            "NATN_CD" to "", // 국가 코드
            "CRCY_CD" to "USD", // 통화 코드
            "PDNO" to "",
            "CTX_AREA_FK200" to "",
            "CTX_AREA_NK200" to ""
        )
    }

}
