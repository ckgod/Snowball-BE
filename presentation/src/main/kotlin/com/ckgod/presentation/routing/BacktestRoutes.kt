package com.ckgod.presentation.routing

import com.ckgod.domain.model.BacktestRequest
import com.ckgod.domain.usecase.BacktestUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * 백테스트 API
 *
 * POST /sb/backtest
 *
 * Body: BacktestRequest JSON
 */
suspend fun RoutingContext.backtestRoutes(
    backtestUseCase: BacktestUseCase
) {
    try {
        val request = call.receive<BacktestRequest>()

        if (request.startDate > request.endDate) {
            return call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "startDate가 endDate보다 클 수 없습니다")
            )
        }

        val result = backtestUseCase.run(request)
        call.respond(result)

    } catch (e: IllegalStateException) {
        call.respond(
            HttpStatusCode.Conflict,
            mapOf("error" to (e.message ?: "백테스트가 이미 실행 중입니다"))
        )
    } catch (e: IllegalArgumentException) {
        call.respond(
            HttpStatusCode.BadRequest,
            mapOf("error" to (e.message ?: "잘못된 요청입니다"))
        )
    } catch (e: Exception) {
        println("Error processing backtest request: ${e.message}")
        e.printStackTrace()
        call.respond(
            HttpStatusCode.InternalServerError,
            mapOf("error" to "백테스트 처리 중 오류가 발생했습니다: ${e.message}")
        )
    }
}
