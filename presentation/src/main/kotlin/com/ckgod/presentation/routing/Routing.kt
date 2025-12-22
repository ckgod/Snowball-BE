package com.ckgod.presentation.routing

import com.ckgod.domain.repository.InvestmentStatusRepository
import com.ckgod.domain.repository.StockRepository
import com.ckgod.domain.repository.TradeHistoryRepository
import com.ckgod.domain.usecase.GetAccountStatusUseCase
import com.ckgod.domain.usecase.GetCurrentPriceUseCase
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(
    getCurrentPriceUseCase: GetCurrentPriceUseCase,
    getAccountStatusUseCase: GetAccountStatusUseCase,
    investmentStatusRepository: InvestmentStatusRepository,
    tradeHistoryRepository: TradeHistoryRepository,
    stockRepository: StockRepository
) {
    routing {
        route("/sb") {
            get("/home/status") {
                mainStatusRoute(investmentStatusRepository, stockRepository)
            }
            get("/account/status") {
                accountRoutes(getAccountStatusUseCase)
            }
            get("/price") {
                currentPriceRoutes(getCurrentPriceUseCase)
            }
            get("/history") {
                historyRoutes(tradeHistoryRepository)
            }
        }
    }
}
