package com.ckgod.presentation.routing

import com.ckgod.domain.repository.InvestmentStatusRepository
import com.ckgod.domain.repository.TradeHistoryRepository
import com.ckgod.domain.usecase.GetAccountStatusUseCase
import com.ckgod.domain.usecase.GetCurrentPriceUseCase
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(
    getCurrentPriceUseCase: GetCurrentPriceUseCase,
    getAccountStatusUseCase: GetAccountStatusUseCase,
    investmentStatusRepository: InvestmentStatusRepository,
    tradeHistoryRepository: TradeHistoryRepository
) {
    routing {
        route("/ckapi/v1") {
            // TODO api endpoint url 여기서 관리하도록 변경
            currentPriceRoutes(getCurrentPriceUseCase)
            accountRoutes(getAccountStatusUseCase)
            mainStatusRoute(investmentStatusRepository)
            historyRoutes(tradeHistoryRepository)
        }
    }
}
