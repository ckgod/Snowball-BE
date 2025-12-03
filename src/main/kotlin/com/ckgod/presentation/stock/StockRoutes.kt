package com.ckgod.presentation.stock

import com.ckgod.config.KisMode
import com.ckgod.domain.stock.StockService
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Route.stockRoutes(stockService: StockService) {
    route("/api/stock") {
        /**
         * GET /api/stock/price?code=005930&mode=MOCK
         * 주식 현재가 조회
         */
        get("/price") {
            val code = call.request.queryParameters["code"]
                ?: return@get call.respondText("종목코드(code)가 누락되었습니다")
            val mode = KisMode.from(call.request.queryParameters["mode"])

            try {
                val response = stockService.getStockPrice(code, mode)
                call.respond(response)
            } catch (e: Exception) {
                e.printStackTrace()
                call.respondText("error: ${e.message}", status = io.ktor.http.HttpStatusCode.InternalServerError)
            }
        }
    }
}
