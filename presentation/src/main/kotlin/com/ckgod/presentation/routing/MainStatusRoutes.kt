package com.ckgod.presentation.routing

import com.ckgod.domain.repository.InvestmentStatusRepository
import com.ckgod.domain.repository.StockRepository
import com.ckgod.presentation.mapper.InvestmentStatusMapper
import com.ckgod.snowball.model.HomeTabResponse
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

suspend fun RoutingContext.mainStatusRoute(
    investmentStatusRepository: InvestmentStatusRepository,
    stockRepository: StockRepository
) {
    val ticker = call.request.queryParameters["ticker"]

    if (ticker != null) {
        val status = investmentStatusRepository.get(ticker)

        if (status == null) {
            call.respond(
                HttpStatusCode.NotFound,
                mapOf("error" to "$ticker 투자 상태가 없습니다.")
            )
            return
        }

        call.respond(HttpStatusCode.OK, InvestmentStatusMapper.toResponse(status, stockRepository))
    } else {
        val allStatuses = investmentStatusRepository.findAll()

        if (allStatuses.isEmpty()) {
            call.respond(
                HttpStatusCode.NotFound,
                mapOf("error" to "투자 중인 종목이 없습니다.")
            )
            return
        }

        var totalProfit = 0.0
        val responses = coroutineScope {
            allStatuses.map { status ->
                totalProfit += status.realizedTotalProfit
                async { InvestmentStatusMapper.toResponse(status, stockRepository) }
            }.awaitAll()
        }

        call.respond(
            HttpStatusCode.OK,
            HomeTabResponse(
                totalProfit = totalProfit,
                statusList = responses
            )
        )
    }

}
