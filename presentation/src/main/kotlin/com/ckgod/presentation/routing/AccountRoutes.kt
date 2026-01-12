package com.ckgod.presentation.routing

import com.ckgod.domain.usecase.GetAccountStatusUseCase
import com.ckgod.presentation.mapper.AccountStatusMapper
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

suspend fun RoutingContext.accountRoutes(
    getAccountStatusUseCase: GetAccountStatusUseCase,
) {
    try {
        val accountStatus = getAccountStatusUseCase(true)
        call.respond(AccountStatusMapper.toResponse(accountStatus))
    } catch (e: IllegalArgumentException) {
        e.printStackTrace()
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