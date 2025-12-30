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
    val status = investmentStateRepository.get(ticker)?.let {
        val marketPrice = stockRepository.getCurrentPrice(ticker)
        val currentPrice = marketPrice?.price?.toDoubleOrNull() ?: 0.0
        val dailyChangeRate = marketPrice?.changeRate?.toDoubleOrNull() ?: 0.0

        InvestmentStatusMapper.toResponse(
            status = it,
            currentPrice = currentPrice,
            dailyChangeRate = dailyChangeRate,
            exchangeRate = marketPrice?.exchangeRate?.toDoubleOrNull()
        )
    }

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