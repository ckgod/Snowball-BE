package com.ckgod.presentation.routing

import com.ckgod.domain.usecase.GetStockPriceHistoryUseCase
import com.ckgod.snowball.model.PriceData
import com.ckgod.snowball.model.StockPriceHistoryResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.LocalDate

/**
 * 주식 가격 히스토리 API
 *
 * GET /sb/stock/history?ticker=TQQQ&startDate=2023-01-01&endDate=2023-12-31
 *
 * 쿼리 파라미터:
 * - ticker: 종목 심볼 (필수)
 * - startDate: 시작일 YYYY-MM-DD (필수)
 * - endDate: 종료일 YYYY-MM-DD (필수)
 */
suspend fun RoutingContext.stockPriceHistoryRoutes(
    getStockPriceHistoryUseCase: GetStockPriceHistoryUseCase
) {
    // 쿼리 파라미터 추출
    val ticker = call.request.queryParameters["ticker"]
        ?: return call.respond(
            HttpStatusCode.BadRequest,
            mapOf("error" to "ticker 파라미터가 필요합니다")
        )

    val startDateStr = call.request.queryParameters["startDate"]
        ?: return call.respond(
            HttpStatusCode.BadRequest,
            mapOf("error" to "startDate 파라미터가 필요합니다 (YYYY-MM-DD)")
        )

    val endDateStr = call.request.queryParameters["endDate"]
        ?: return call.respond(
            HttpStatusCode.BadRequest,
            mapOf("error" to "endDate 파라미터가 필요합니다 (YYYY-MM-DD)")
        )

    try {
        // 날짜 파싱
        val startDate = LocalDate.parse(startDateStr)
        val endDate = LocalDate.parse(endDateStr)

        // 날짜 범위 검증
        if (startDate > endDate) {
            return call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "startDate가 endDate보다 클 수 없습니다")
            )
        }

        // 가격 히스토리 조회
        val priceHistory = getStockPriceHistoryUseCase(ticker.uppercase(), startDate, endDate)

        // 응답 변환
        val response = StockPriceHistoryResponse(
            ticker = ticker.uppercase(),
            prices = priceHistory.map { price ->
                PriceData(
                    date = price.date.toString(),
                    open = price.open,
                    high = price.high,
                    low = price.low,
                    close = price.close,
                    adjClose = price.adjClose,
                    volume = price.volume
                )
            }
        )

        call.respond(response)

    } catch (e: IllegalArgumentException) {
        call.respond(
            HttpStatusCode.BadRequest,
            mapOf("error" to "날짜 형식이 잘못되었습니다 (YYYY-MM-DD)")
        )
    } catch (e: Exception) {
        println("Error processing stock price history request: ${e.message}")
        e.printStackTrace()
        call.respond(
            HttpStatusCode.InternalServerError,
            mapOf("error" to "요청 처리 중 오류가 발생했습니다")
        )
    }
}
