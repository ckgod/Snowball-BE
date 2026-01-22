package com.ckgod.presentation.routing

import com.ckgod.domain.repository.AccountRepository
import com.ckgod.domain.repository.InvestmentStatusRepository
import com.ckgod.domain.repository.StockRepository
import com.ckgod.presentation.mapper.TotalAssetMapper
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

suspend fun RoutingContext.accountRoutes(
    investmentStatusRepository: InvestmentStatusRepository,
    accountRepository: AccountRepository,
    stockRepository: StockRepository,
) {
    try {
        coroutineScope {
            val accountStatusDeferred = async { accountRepository.getPresentAccountBalance() }
            val totalAssetDeferred = async { accountRepository.getTotalAsset() }
            val investmentStatusDeferred = async { investmentStatusRepository.findAll() }
            val exchangeRateDeferred = async { stockRepository.getExchangeRate() }

            val accountStatus = accountStatusDeferred.await()
            val totalAsset = totalAssetDeferred.await()
            val capitalList = investmentStatusDeferred.await().map { status ->
                status.ticker to status.initialCapital
            }
            val exchangeRate = exchangeRateDeferred.await()

            call.respond(TotalAssetMapper.toResponse(
                accountStatus = accountStatus,
                totalAsset = totalAsset,
                capitalList = capitalList,
                exchangeRate = exchangeRate,
            ))
        }
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