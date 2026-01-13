package com.ckgod.presentation.routing

import com.ckgod.domain.repository.InvestmentStatusRepository
import com.ckgod.domain.usecase.GetAccountStatusUseCase
import com.ckgod.presentation.mapper.AccountStatusMapper
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

suspend fun RoutingContext.accountRoutes(
    getAccountStatusUseCase: GetAccountStatusUseCase,
    investmentStatusRepository: InvestmentStatusRepository,
) {
    try {
        val accountStatus = getAccountStatusUseCase(true)
        val capitalList = investmentStatusRepository.findAll().map { status ->
            status.ticker to status.initialCapital
        }
        call.respond(AccountStatusMapper.toResponse(accountStatus, capitalList))
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