package com.ckgod.kis.api

import com.ckgod.domain.model.OrderRequest
import com.ckgod.domain.model.OrderSide
import com.ckgod.domain.utils.beforeDay
import com.ckgod.domain.utils.yesterday
import com.ckgod.kis.KisApiClient
import com.ckgod.kis.KisResponseWithHeaders
import com.ckgod.kis.spec.KisApiSpec
import com.ckgod.kis.request.KisOrderRequest
import com.ckgod.kis.response.KisBalanceResponse
import com.ckgod.kis.response.KisDateProfitResponse
import com.ckgod.kis.response.KisExecutionResponse
import com.ckgod.kis.response.KisOrderResponse
import com.ckgod.kis.response.KisPresentBalanceResponse
import com.ckgod.kis.response.KisPriceResponse
import com.ckgod.kis.response.KisTotalAssetResponse

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
            startDate = beforeDay(3),
            endDate = yesterday()
        )

        return apiClient.request<KisDateProfitResponse, Unit>(
            spec = spec,
            queryParams = queryParams
        )
    }

    /**
     * 해외주식 현재가 조회.
     *
     * @param includeDayMarket true면 주간거래(데이마켓) 세션 중에는 주간 EXCD로 먼저 조회하고,
     *   응답이 비어 있으면 정규장 EXCD로 폴백한다. 대시보드 조회용. (기본 false)
     *
     * 주의: 주문 생성(LOC/MOC는 정규장 종가 체결)·환율 조회는 반드시 정규장 기준이어야 하므로
     *   기본값 false를 유지하고 includeDayMarket을 넘기지 않는다.
     */
    suspend fun getMarketCurrentPrice(
        stockCode: String,
        includeDayMarket: Boolean = false
    ): KisPriceResponse {
        val exchange = UsExchange.of(stockCode)

        if (includeDayMarket && UsDayMarketSession.isLikelyOpen()) {
            val dayResponse = requestPrice(stockCode, exchange.day)
            if (!dayResponse.output?.currentPrice.isNullOrBlank()) {
                return dayResponse
            }
        }

        return requestPrice(stockCode, exchange.regular)
    }

    private suspend fun requestPrice(stockCode: String, excd: String): KisPriceResponse {
        val spec = KisApiSpec.QuotationPriceDetail
        val queryParams = spec.buildQuery(
            userId = apiClient.config.userId,
            exchange = excd,
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

    suspend fun getPresentAccountBalance() : KisPresentBalanceResponse {
        val spec = KisApiSpec.InquirePresentBalance
        val params = spec.buildQuery(
            accountNo = apiClient.config.accountNo,
            accountCode = apiClient.config.accountCode
        )

        return apiClient.request<KisPresentBalanceResponse, Unit>(
            spec = spec,
            queryParams = params
        )
    }

    suspend fun getExecution(trCont: String = "", fKey: String = "", nKey: String = ""): KisResponseWithHeaders<KisExecutionResponse> {
        val spec = KisApiSpec.InquireCCnl()
        val queryParams = spec.buildQuery(
            accountNo = apiClient.config.accountNo,
            accountCode = apiClient.config.accountCode,
            startDate = yesterday(),
            endDate = yesterday(),
            nKey = nKey,
            fKey = fKey
        )

        return apiClient.requestWithHeaders<KisExecutionResponse, Unit>(
            spec = spec,
            queryParams = queryParams,
            additionalHeaders = mapOf("tr_cont" to trCont)
        )
    }

    suspend fun getTotalAsset(): KisTotalAssetResponse {
        val spec = KisApiSpec.TotalAsset
        val queryParams = spec.buildQuery(
            accountNo = apiClient.config.accountNo,
            accountCode = apiClient.config.accountCode,
        )

        return apiClient.request<KisTotalAssetResponse, Unit>(
            spec = spec,
            queryParams = queryParams
        )
    }
}
