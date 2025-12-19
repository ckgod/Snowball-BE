package com.ckgod.presentation.routing

import com.ckgod.domain.repository.TradeHistoryRepository
import com.ckgod.presentation.response.HistoryItem
import com.ckgod.presentation.response.HistoryResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlin.text.toIntOrNull


/**
 * GET /api/v1/history
 *
 * 앱 히스토리 화면용: 과거 거래 내역 (페이징)
 */
fun Route.historyRoutes(
    tradeHistoryRepository: TradeHistoryRepository
) {
    get("/history") {
        val ticker = call.request.queryParameters["ticker"]
        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100

        val histories = if (ticker != null) {
            tradeHistoryRepository.findByTicker(ticker, limit)
        } else {
            tradeHistoryRepository.findAll(limit)
        }

        call.respond(
            HttpStatusCode.OK,
            HistoryResponse(
                total = histories.size,
                histories = histories.map { history ->
                    HistoryItem(
                        id = history.id,
                        ticker = history.ticker,
                        orderNo = history.orderNo,
                        orderSide = history.orderSide.name,
                        orderType = history.orderType.name,
                        orderPrice = history.orderPrice,
                        orderQuantity = history.orderQuantity,
                        orderTime = history.orderTime,
                        status = history.status.name,
                        filledQuantity = history.filledQuantity,
                        filledPrice = history.filledPrice,
                        filledTime = history.filledTime,
                        tValue = history.tValue,
                        createdAt = history.createdAt
                    )
                }
            )
        )
    }
}