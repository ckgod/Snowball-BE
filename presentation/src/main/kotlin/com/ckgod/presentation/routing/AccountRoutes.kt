package com.ckgod.presentation.routing

import com.ckgod.domain.repository.AccountRepository
import com.ckgod.domain.repository.InvestmentStatusRepository
import com.ckgod.domain.repository.StockRepository
import com.ckgod.presentation.mapper.TotalAssetMapper
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

            // 잔고 API 의 현재가는 정규장 중간 값에 멈춰 있기도 해서, 투자 현황과 같은 시세(주간거래 포함)로 다시 평가한다
            val prices = accountStatusDeferred.await().holdings.map { holding ->
                async {
                    val price = runCatching {
                        stockRepository.getCurrentPrice(holding.ticker, includeDayMarket = true)?.price?.toDoubleOrNull()
                    }.getOrNull()
                    holding.ticker to price
                }
            }.awaitAll().mapNotNull { (ticker, price) -> price?.takeIf { it > 0 }?.let { ticker to it } }.toMap()

            val accountStatus = TotalAssetMapper.repriced(accountStatusDeferred.await(), prices)
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