package com.ckgod.presentation.routing

import com.ckgod.domain.repository.InvestmentStatusRepository
import com.ckgod.domain.repository.StockRepository
import com.ckgod.domain.repository.TradeHistoryRepository
import com.ckgod.presentation.mapper.InvestmentStatusMapper
import com.ckgod.snowball.model.TradeHistoryUiModel
import com.ckgod.snowball.model.StockDetailUiModel
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

        InvestmentStatusMapper.toUiModel(
            status = it,
            currentPrice = currentPrice,
            dailyChangeRate = dailyChangeRate,
            exchangeRate = marketPrice?.exchangeRate?.toDoubleOrNull()
        )
    }

    call.respond(
        HttpStatusCode.OK,
        StockDetailUiModel(
            status = status,
            histories = histories.map { history ->
                TradeHistoryUiModel(
                    id = history.id,
                    ticker = history.ticker,
                    orderNo = history.orderNo,
                    orderSide = history.orderSide.name,
                    orderType = history.orderType.name,
                    orderPrice = history.orderPrice,
                    orderQuantity = history.orderQuantity,
                    orderTime = history.orderTime.toString(),
                    status = history.status.name,
                    filledQuantity = history.filledQuantity,
                    filledPrice = history.filledPrice,
                    filledTime = history.filledTime?.toString(),
                    tValue = history.tValue,
                    createdAt = history.createdAt.toString()
                )
            }
        )
    )

}