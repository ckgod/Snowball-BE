package com.ckgod.presentation.routing

import com.ckgod.domain.repository.InvestmentStatusRepository
import com.ckgod.domain.repository.StockRepository
import com.ckgod.domain.repository.TradeHistoryRepository
import com.ckgod.presentation.mapper.InvestmentStatusMapper
import com.ckgod.presentation.mapper.TradeHistoryMapper
import com.ckgod.snowball.model.StockDetailResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import kotlin.text.toIntOrNull

suspend fun RoutingContext.stockDetailRoutes(
    tradeHistoryRepository: TradeHistoryRepository,
    investmentStateRepository: InvestmentStatusRepository,
    stockRepository: StockRepository,
) {
    val ticker = call.request.queryParameters["ticker"]
    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100

    if (ticker == null) {
        call.respond(
            HttpStatusCode.BadRequest,
            mapOf("error" to "ticker 정보가 없습니다.")
        )
        return
    }
    val histories = tradeHistoryRepository.findByTicker(ticker, limit)
    val status = investmentStateRepository.get(ticker)?.let { InvestmentStatusMapper.toResponse(it, stockRepository) }

    call.respond(
        HttpStatusCode.OK,
        StockDetailResponse(
            status = status,
            histories = histories.map { history ->
                TradeHistoryMapper.toResponse(history)
            }
        )
    )

}