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
        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100

        val histories = tradeHistoryRepository.findAll(limit)

        call.respond(
            HttpStatusCode.OK,
            HistoryResponse(
                total = histories.size,
                histories = histories.map { history ->
                    HistoryItem(
                        id = history.id,
                        date = history.date,
                        type = history.type.name,
                        price = history.price,
                        quantity = history.quantity,
                        profit = history.profit,
                        tValueAt = history.tValueAt
                    )
                }
            )
        )
    }
}