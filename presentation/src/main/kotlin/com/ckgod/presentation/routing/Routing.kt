package com.ckgod.presentation.routing

import com.ckgod.domain.repository.AccountRepository
import com.ckgod.domain.repository.InvestmentStatusRepository
import com.ckgod.domain.repository.StockRepository
import com.ckgod.domain.repository.TradeHistoryRepository
import com.ckgod.domain.usecase.BacktestUseCase
import com.ckgod.domain.usecase.GetCurrentPriceUseCase
import com.ckgod.domain.usecase.GetStockPriceHistoryUseCase
import com.ckgod.domain.usecase.ManageOrdersUseCase
import com.ckgod.domain.usecase.PlaceOrderUseCase
import com.ckgod.presentation.config.OrderGuard
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(
    getCurrentPriceUseCase: GetCurrentPriceUseCase,
    getStockPriceHistoryUseCase: GetStockPriceHistoryUseCase,
    investmentStatusRepository: InvestmentStatusRepository,
    tradeHistoryRepository: TradeHistoryRepository,
    stockRepository: StockRepository,
    accountRepository: AccountRepository,
    backtestUseCase: BacktestUseCase,
    manageOrdersUseCase: ManageOrdersUseCase,
    placeOrderUseCase: PlaceOrderUseCase,
    orderGuard: OrderGuard
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
            get("/stock/history") {
                stockPriceHistoryRoutes(getStockPriceHistoryUseCase)
            }
            get("/stock/detail") {
                stockDetailRoutes(
                    tradeHistoryRepository,
                    investmentStatusRepository,
                    stockRepository
                )
            }
            post("/backtest") {
                backtestRoutes(backtestUseCase)
            }
            post("/orders") {
                placeOrderRoute(placeOrderUseCase, orderGuard)
            }
            get("/orders/open") {
                openOrdersRoute(manageOrdersUseCase)
            }
            post("/orders/{orderNo}/cancel") {
                cancelOrderRoute(manageOrdersUseCase, orderGuard)
            }
            post("/orders/{orderNo}/modify") {
                modifyOrderRoute(manageOrdersUseCase, orderGuard)
            }
        }
    }
}
