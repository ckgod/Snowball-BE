package com.ckgod.presentation.routing

import com.ckgod.domain.model.InvestmentStatus
import com.ckgod.domain.repository.InvestmentStatusRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlin.text.toDoubleOrNull

/**
 * POST /api/v1/init?ticker=TQQQ&capital=20000
 *
 * 새 종목 추가
 */
fun Route.investmentRoutes(
    investmentStatusRepository: InvestmentStatusRepository
) {
    post("/investment/enroll") {
        val ticker = call.request.queryParameters["ticker"]

        if (ticker == null) {
            call.respond(HttpStatusCode.BadRequest,
                mapOf("error" to "투자 종목명을 입력해주세요."))
            return@post
        }

        val initialCapital = call.request.queryParameters["capital"]?.toDoubleOrNull() ?: 20000.0

        // 이미 해당 ticker가 존재하는지 확인
        val existing = investmentStatusRepository.get(ticker)
        if (existing != null) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "${ticker}는 이미 초기화되었습니다.")
            )
            return@post
        }

        val status = InvestmentStatus.create(ticker, initialCapital)
        investmentStatusRepository.save(status)

        call.respond(
            HttpStatusCode.Created,
            mapOf(
                "message" to "초기화 완료",
                "ticker" to ticker,
                "capital" to initialCapital
            )
        )
    }
}
