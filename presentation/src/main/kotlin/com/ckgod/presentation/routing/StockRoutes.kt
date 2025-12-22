package com.ckgod.presentation.routing

import com.ckgod.domain.usecase.GetCurrentPriceUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * GET /ckapi/price?exchange=NAS&code=TQQQ
 */
suspend fun RoutingContext.currentPriceRoutes(
    getCurrentPriceUseCase: GetCurrentPriceUseCase
) {
    val code = call.request.queryParameters["code"]
        ?: return call.respond(
            HttpStatusCode.BadRequest,
            mapOf("error" to "종목 코드가 필요합니다")
        )

    try {
        val marketPrice = getCurrentPriceUseCase(code)
            ?: return call.respond(
                HttpStatusCode.NotFound,
                mapOf("error" to "존재하지 않는 종목 코드입니다")
            )

        call.respond(marketPrice)
    } catch (e: IllegalArgumentException) {
        call.respond(
            HttpStatusCode.BadRequest,
            mapOf("error" to "잘못된 요청입니다")
        )
    } catch (e: Exception) {
        println("Error processing stock price request: ${e.message}")
        e.printStackTrace()
        call.respond(
            HttpStatusCode.InternalServerError,
            mapOf("error" to "요청 처리 중 오류가 발생했습니다")
        )
    }
}
