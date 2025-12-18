package com.ckgod.presentation.routing

import com.ckgod.domain.repository.InvestmentStatusRepository
import com.ckgod.domain.repository.StockRepository
import com.ckgod.presentation.response.StatusListResponse
import com.ckgod.presentation.response.StatusResponse
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * GET /api/v1/status?ticker=TQQQ
 * GET /api/v1/status (모든 종목)
 *
 * 앱 메인 화면용: 현재 T값, 별%, 주문 가격 등
 */
fun Route.mainStatusRoute(
    investmentStatusRepository: InvestmentStatusRepository,
    stockRepository: StockRepository
) {
    get("/main/status") {
        val ticker = call.request.queryParameters["ticker"]

        if (ticker != null) {
            // 단일 종목 조회
            val status = investmentStatusRepository.get(ticker)

            if (status == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "$ticker 투자 상태가 없습니다.")
                )
                return@get
            }

            // 현재가 조회
            val marketPrice = stockRepository.getCurrentPrice(ticker)
            val currentPrice = marketPrice?.price?.toDoubleOrNull() ?: 0.0
            val dailyChangeRate = marketPrice?.changeRate?.toDoubleOrNull() ?: 0.0

            val response = StatusResponse.from(
                status = status,
                currentPrice = currentPrice,
                dailyChangeRate = dailyChangeRate,
                exchangeRate = marketPrice?.exchangeRate?.toDoubleOrNull()
            )

            call.respond(HttpStatusCode.OK, response)
        } else {
            // 전체 종목 조회
            val allStatuses = investmentStatusRepository.findAll()

            if (allStatuses.isEmpty()) {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "투자 중인 종목이 없습니다.")
                )
                return@get
            }

            // 모든 종목의 현재가를 병렬로 조회하고 StatusResponse 생성
            val responses = coroutineScope {
                allStatuses.map { status ->
                    async {
                        val marketPrice = stockRepository.getCurrentPrice(status.ticker)
                        val currentPrice = marketPrice?.price?.toDoubleOrNull() ?: 0.0
                        val dailyChangeRate = marketPrice?.changeRate?.toDoubleOrNull() ?: 0.0

                        StatusResponse.from(
                            status = status,
                            currentPrice = currentPrice,
                            dailyChangeRate = dailyChangeRate,
                            exchangeRate = marketPrice?.exchangeRate?.toDoubleOrNull()
                        )
                    }
                }.awaitAll()
            }

            call.respond(
                HttpStatusCode.OK,
                StatusListResponse(
                    total = responses.size,
                    statusList = responses
                )
            )
        }
    }
}
