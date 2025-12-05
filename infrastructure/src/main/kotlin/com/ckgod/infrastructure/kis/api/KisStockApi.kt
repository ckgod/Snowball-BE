package com.ckgod.infrastructure.kis.api

import com.ckgod.infrastructure.kis.KisApiClient
import com.ckgod.infrastructure.kis.api.spec.KisApiSpec
import com.ckgod.infrastructure.kis.response.KisStockPriceResponse

class KisStockApi(private val apiClient: KisApiClient) {

    suspend fun getStockPrice(
        stockCode: String,
        marketDivCode: String = "J"
    ): KisStockPriceResponse {
        val spec = KisApiSpec.InquirePrice
        val queryParams = spec.buildQuery(stockCode, marketDivCode)

        val response = apiClient.request(
            spec = spec,
            queryParams = queryParams
        )

        return KisStockPriceResponse.from(response)
    }
}
