package com.ckgod.presentation.routing

import com.ckgod.domain.repository.AccountRepository
import com.ckgod.domain.repository.InvestmentStatusRepository
import com.ckgod.domain.repository.StockRepository
import com.ckgod.domain.repository.TradeHistoryRepository
import com.ckgod.domain.usecase.GetCurrentPriceUseCase
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(
    getCurrentPriceUseCase: GetCurrentPriceUseCase,
    investmentStatusRepository: InvestmentStatusRepository,
    tradeHistoryRepository: TradeHistoryRepository,
    stockRepository: StockRepository,
    accountRepository: AccountRepository
) {
    routing {
        route("/sb") {
            get("/home/status") {
                mainStatusRoute(investmentStatusRepository, stockRepository)
            }
            get("/account/status") {
                accountRoutes(investmentStatusRepository, accountRepository, stockRepository)
            }
            get("/stock/price") {
                stockPriceRoutes(getCurrentPriceUseCase)
            }
            get("/stock/detail") {
                stockDetailRoutes(
                    tradeHistoryRepository,
                    investmentStatusRepository,
                    stockRepository
                )
            }
        }
    }
}
