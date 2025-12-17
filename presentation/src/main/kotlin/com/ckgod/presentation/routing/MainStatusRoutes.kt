package com.ckgod.presentation.routing

import com.ckgod.domain.repository.InvestmentStatusRepository
import com.ckgod.presentation.response.StatusListResponse
import com.ckgod.presentation.response.StatusResponse
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * GET /api/v1/status?ticker=TQQQ
 * GET /api/v1/status (모든 종목)
 *
 * 앱 메인 화면용: 현재 T값, 별%, 주문 가격 등
 */
fun Route.mainStatusRoute(
    investmentStatusRepository: InvestmentStatusRepository
) {
    get("/main/status") {
        val ticker = call.request.queryParameters["ticker"]

        if (ticker != null) {
            val status = investmentStatusRepository.get(ticker)

            if (status == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "$ticker 투자 상태가 없습니다.")
                )
                return@get
            }

            call.respond(
                HttpStatusCode.OK,
                StatusResponse(
                    ticker = status.ticker,
                    currentT = status.tValue,
                    targetRate = status.targetRate,
                    avgPrice = status.avgPrice,
                    buyLocPrice = status.buyLocPrice,
                    sellLocPrice = status.sellLocPrice,
                    oneTimeAmount = status.oneTimeAmount,
                    totalInvested = status.totalInvested,
                    updatedAt = status.updatedAt
                )
            )
        } else {
            val allStatuses = investmentStatusRepository.findAll()

            if (allStatuses.isEmpty()) {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "투자 중인 종목이 없습니다.")
                )
                return@get
            }

            call.respond(
                HttpStatusCode.OK,
                StatusListResponse(
                    total = allStatuses.size,
                    statusList = allStatuses.map { status ->
                        StatusResponse(
                            ticker = status.ticker,
                            currentT = status.tValue,
                            targetRate = status.targetRate,
                            avgPrice = status.avgPrice,
                            buyLocPrice = status.buyLocPrice,
                            sellLocPrice = status.sellLocPrice,
                            oneTimeAmount = status.oneTimeAmount,
                            totalInvested = status.totalInvested,
                            updatedAt = status.updatedAt
                        )
                    }
                )
            )
        }
    }
}
