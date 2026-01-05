package com.ckgod

import com.ckgod.database.DatabaseFactory
import com.ckgod.di.configureKoin
import com.ckgod.domain.repository.InvestmentStatusRepository
import com.ckgod.domain.repository.StockRepository
import com.ckgod.domain.repository.TradeHistoryRepository
import com.ckgod.domain.usecase.GetAccountStatusUseCase
import com.ckgod.domain.usecase.GetCurrentPriceUseCase
import com.ckgod.presentation.config.configureAuthPlugin
import com.ckgod.presentation.config.configureRateLimiter
import com.ckgod.presentation.config.configureSerialization
import com.ckgod.presentation.routing.configureRouting
import com.ckgod.scheduler.SchedulerService
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.koin.ktor.ext.get
import org.koin.ktor.ext.inject

lateinit var simpleScheduler: SchedulerService

fun main() {
    val server = embeddedServer(Netty, port = 8080, module = Application::mainModule)

    Runtime.getRuntime().addShutdownHook(Thread {
        println("서버 종료 중...")
        if (::simpleScheduler.isInitialized) {
            simpleScheduler.stop()
        }
    })

    server.start(wait = true)
}

fun Application.mainModule() {
    // ============= DI ============
    configureKoin()

    // ========== Database ==========
    DatabaseFactory.init()

    // ========== Security ==========
    val config = environment.config
    val apiKey = config.property("api.key").getString()
    val maxRequests = config.property("api.rateLimit.maxRequests").getString().toInt()
    val windowSeconds = config.property("api.rateLimit.windowSeconds").getString().toLong()

    // ========== Server Configuration ==========
    configureAuthPlugin(apiKey)
    configureRateLimiter(maxRequests, windowSeconds)
    configureSerialization()

    // ========== Use Case ===========
    val getCurrentPriceUseCase: GetCurrentPriceUseCase by inject()
    val getAccountStatusUseCase: GetAccountStatusUseCase by inject()
    val investmentStatusRepository by inject<InvestmentStatusRepository>()
    val tradeHistoryRepository by inject<TradeHistoryRepository>()
    val stockRepository by inject<StockRepository>()

    // ========== Scheduler ==========
    simpleScheduler = get()
    simpleScheduler.start()

    // ========== API Routing ==========
    configureRouting(
        getCurrentPriceUseCase = getCurrentPriceUseCase,
        getAccountStatusUseCase = getAccountStatusUseCase,
        investmentStatusRepository = investmentStatusRepository,
        tradeHistoryRepository = tradeHistoryRepository,
        stockRepository = stockRepository
    )
}
