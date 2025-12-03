package com.ckgod

import com.ckgod.domain.stock.StockService
import com.ckgod.presentation.stock.stockRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(stockService: StockService) {
    routing {
        stockRoutes(stockService)
    }
}
