package com.ckgod.kis.api

import com.ckgod.domain.model.OrderRequest
import com.ckgod.domain.model.OrderSide
import com.ckgod.kis.KisApiClient
import com.ckgod.kis.KisResponseWithHeaders
import com.ckgod.kis.spec.KisApiSpec
import com.ckgod.kis.request.KisOrderRequest
import com.ckgod.kis.response.KisBalanceResponse
import com.ckgod.kis.response.KisDateProfitResponse
import com.ckgod.kis.response.KisExecutionResponse
import com.ckgod.kis.response.KisOrderResponse
import com.ckgod.kis.response.KisPriceResponse
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class KisApiService(private val apiClient: KisApiClient) {

    suspend fun postOrder(request: OrderRequest): KisOrderResponse {
        val spec = when(request.side) {
            OrderSide.SELL -> KisApiSpec.SellOrder
            OrderSide.BUY -> KisApiSpec.BuyOrder
        }
        val body = KisOrderRequest.from(apiClient.config, request)

        return apiClient.request(spec, bodyParams = body)
    }

    suspend fun getRecentDayProfit(): KisDateProfitResponse {
        val spec = KisApiSpec.InquirePeriodProfit

        val queryParams = spec.buildQuery(
            accountNo = apiClient.config.accountNo,
            accountCode = apiClient.config.accountCode,
            startDate = yesterday(),
            endDate = yesterday()
        )

        return apiClient.request<KisDateProfitResponse, Unit>(
            spec = spec,
            queryParams = queryParams
        )
    }

    suspend fun getMarketCurrentPrice(
        stockCode: String
    ): KisPriceResponse {
        val spec = KisApiSpec.QuotationPriceDetail
        val exchange = when(stockCode) {
            "TQQQ" -> "NAS"
            "SOXL", "FNGU" -> "AMS"
            else -> "NAS"
        }
        val queryParams = spec.buildQuery(
            userId = apiClient.config.userId,
            exchange = exchange,
            stockCode = stockCode
        )

        return apiClient.request<KisPriceResponse, Unit>(
            spec = spec,
            queryParams = queryParams
        )
    }

    suspend fun getAccountBalance() : KisBalanceResponse {
        val spec = KisApiSpec.InquireBalance
        val queryParams = spec.buildQuery(
            accountNo = apiClient.config.accountNo,
            accountCode = apiClient.config.accountCode
        )

        return apiClient.request<KisBalanceResponse, Unit>(
            spec = spec,
            queryParams = queryParams
        )
    }

    suspend fun getExecution(trCont: String = "", fKey: String = "", nKey: String = ""): KisResponseWithHeaders<KisExecutionResponse> {
        val spec = KisApiSpec.InquireCCnl()
        val queryParams = spec.buildQuery(
            accountNo = apiClient.config.accountNo,
            accountCode = apiClient.config.accountCode,
            startDate = beforeDay(6),
            endDate = today(),
            nKey = nKey,
            fKey = fKey
        )

        return apiClient.requestWithHeaders<KisExecutionResponse, Unit>(
            spec = spec,
            queryParams = queryParams,
            additionalHeaders = mapOf("tr_cont" to trCont)
        )
    }

    private fun yesterday(): String {
        val kstZone = ZoneId.of("Asia/Seoul")
        val yesterday = LocalDate.now(kstZone).minusDays(1)
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        return yesterday.format(formatter)
    }

    private fun beforeDay(day: Long): String {
        val kstZone = ZoneId.of("Asia/Seoul")
        val yesterday = LocalDate.now(kstZone).minusDays(day)
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        return yesterday.format(formatter)
    }

    private fun today(): String {
        val kstZone = ZoneId.of("Asia/Seoul")
        val yesterday = LocalDate.now(kstZone)
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        return yesterday.format(formatter)
    }
}
