package com.ckgod.infrastructure.kis.api

import com.ckgod.infrastructure.kis.KisApiClient
import com.ckgod.infrastructure.kis.dto.KisStockPriceDto

class KisStockApi(private val apiClient: KisApiClient) {

    suspend fun getStockPrice(stockCode: String): KisStockPriceDto {
        val response = apiClient.get(
            path = "/uapi/domestic-stock/v1/quotations/inquire-price",
            trId = "FHKST01010100"
        ) {
            url {
                parameters.append("FID_COND_MRKT_DIV_CODE", "J")
                parameters.append("FID_INPUT_ISCD", stockCode)
            }
        }

        return KisStockPriceDto.from(response)
    }
}
