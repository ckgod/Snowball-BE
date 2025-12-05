package com.ckgod.service

import com.ckgod.config.KisMode
import com.ckgod.infrastructure.kis.api.KisStockApi
import com.ckgod.endpoint.response.StockPriceResponse

class StockService(
    private val realStockApi: KisStockApi,
    private val mockStockApi: KisStockApi
) {
    suspend fun getStockPrice(stockCode: String, mode: KisMode): StockPriceResponse {
        val api = when (mode) {
            KisMode.REAL -> realStockApi
            KisMode.MOCK -> mockStockApi
        }
        val kisData = api.getStockPrice(stockCode)

        return StockPriceResponse(
            code = stockCode,
            name = "종목명(추후구현)",
            currentPrice = kisData.stckPrpr,
            changeRate = kisData.prdyCtrt,
            volume = kisData.acmlVol
        )
    }
}
