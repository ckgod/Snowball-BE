package com.ckgod

import com.ckgod.database.DatabaseFactory
import com.ckgod.database.InvestmentStatusRepositoryImpl
import com.ckgod.database.TradeHistoryRepositoryImpl
import com.ckgod.kis.KisApiClient
import com.ckgod.kis.auth.KisAuthService
import com.ckgod.kis.config.KisConfig
import com.ckgod.kis.config.KisMode
import com.ckgod.kis.api.KisApiService
import com.ckgod.kis.repository.AccountRepositoryImpl
import com.ckgod.kis.repository.StockRepositoryImpl
import com.ckgod.presentation.config.configureAuthPlugin
import com.ckgod.presentation.config.configureRateLimiter
import com.ckgod.presentation.config.configureSerialization
import com.ckgod.scheduler.SchedulerService
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.serialization.json.Json
import com.ckgod.database.auth.AuthTokenRepository
import com.ckgod.domain.usecase.GenerateOrdersUseCase
import com.ckgod.domain.usecase.GetAccountStatusUseCase
import com.ckgod.domain.usecase.GetCurrentPriceUseCase
import com.ckgod.domain.usecase.SyncStrategyUseCase
import com.ckgod.kis.websokets.KisWebSocketsService
import com.ckgod.presentation.routing.configureRouting
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import kotlin.time.Duration.Companion.seconds

lateinit var simpleScheduler: SchedulerService
lateinit var webSocketsService: KisWebSocketsService

/**
 * 간단한 무한매수법 서버
 *
 * 핵심 구조:
 * - 2개 테이블: investment_status, trade_history
 * - 2개 스케줄러: 오전 7시 정산, 오후 6시 주문
 * - 2개 API: GET /status, GET /history
 */
fun main() {
    val server = embeddedServer(Netty, port = 8080, module = Application::simpleModule)

    Runtime.getRuntime().addShutdownHook(Thread {
        println("서버 종료 중...")
        if (::simpleScheduler.isInitialized) {
            simpleScheduler.stop()
        }
        if (::webSocketsService.isInitialized) {
            webSocketsService.stop()
        }
    })

    server.start(wait = true)
}

@Suppress("unused")
fun Application.mainModule() {
    simpleModule()
}

fun Application.simpleModule() {
    // ========== Database ==========
    DatabaseFactory.init()

    val config = environment.config

    // ========== HTTP Client ==========
    val httpClient = HttpClient(CIO) {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.BODY
        }
        install(ContentNegotiation) {
            json(json)
        }
        install(WebSockets) {
            pingInterval = 20.seconds
            contentConverter = KotlinxWebsocketSerializationConverter(json)
        }
    }

    // ========== Security ==========
    val apiKey = config.property("api.key").getString()
    val maxRequests = config.property("api.rateLimit.maxRequests").getString().toInt()
    val windowSeconds = config.property("api.rateLimit.windowSeconds").getString().toLong()

    // ========== KIS Config ==========
    val realConfig = KisConfig(
        mode = KisMode.REAL,
        baseUrl = config.property("kis.real.baseUrl").getString().trim(),
        appKey = config.property("kis.real.appKey").getString().trim(),
        appSecret = config.property("kis.real.appSecret").getString().trim(),
        accountNo = config.property("kis.real.accountNo").getString().trim(),
        accountCode = config.property("kis.real.accountCode").getString().trim(),
        userId = config.property("kis.userId").getString().trim()
    )
    val mockConfig = KisConfig(
        mode = KisMode.MOCK,
        baseUrl = config.property("kis.mock.baseUrl").getString().trim(),
        appKey = config.property("kis.mock.appKey").getString().trim(),
        appSecret = config.property("kis.mock.appSecret").getString().trim(),
        accountNo = config.property("kis.mock.accountNo").getString().trim(),
        accountCode = config.property("kis.mock.accountCode").getString().trim(),
        userId = config.property("kis.userId").getString().trim()
    )

    // ========== Repositories ==========
    val authTokenRepository = AuthTokenRepository()
    val authService = KisAuthService(realConfig, httpClient, authTokenRepository)
    val apiClient = KisApiClient(realConfig, authService, httpClient)
    val apiService = KisApiService(apiClient)

    val stockRepository = StockRepositoryImpl(apiService)
    val accountRepository = AccountRepositoryImpl(apiService)

    val investmentStatusRepository = InvestmentStatusRepositoryImpl()
    val tradeHistoryRepository = TradeHistoryRepositoryImpl()

    // ========== UseCases ==========
    val getAccountStatusUseCase = GetAccountStatusUseCase(
        repository = accountRepository
    )

    val getCurrentPriceUseCase = GetCurrentPriceUseCase(
        repository = stockRepository
    )

    val syncStrategyUseCase = SyncStrategyUseCase(
        accountRepository = accountRepository,
        investmentStatusRepository = investmentStatusRepository
    )

    val generateOrdersUseCase = GenerateOrdersUseCase(
        stockRepository = stockRepository,
        accountRepository = accountRepository,
        investmentStatusRepository = investmentStatusRepository,
        tradeHistoryRepository = tradeHistoryRepository
    )

    // ========== Scheduler ==========
    simpleScheduler = SchedulerService(
        syncStrategyUseCase = syncStrategyUseCase,
        generateOrdersUseCase = generateOrdersUseCase
    )
    simpleScheduler.start()

    // ========== Server Configuration ==========
    configureAuthPlugin(apiKey)
    configureRateLimiter(maxRequests, windowSeconds)
    configureSerialization()

    // ========== API Routing ==========
    configureRouting(
        getCurrentPriceUseCase = getCurrentPriceUseCase,
        getAccountStatusUseCase = getAccountStatusUseCase,
        investmentStatusRepository = investmentStatusRepository,
        tradeHistoryRepository = tradeHistoryRepository,
        stockRepository = stockRepository
    )

    // ========== WebSockets Connection =========
    if (config.property("app.environment").getString().trim() == "production") {
        webSocketsService = KisWebSocketsService(
            config = realConfig,
            authService = authService,
            httpClient = httpClient,
            tradeHistoryRepository = tradeHistoryRepository,
        )
        webSocketsService.start()
    } else {
        val mockAuthService = KisAuthService(mockConfig, httpClient, authTokenRepository)
        webSocketsService = KisWebSocketsService(
            config = mockConfig,
            authService = mockAuthService,
            httpClient = httpClient,
            tradeHistoryRepository = tradeHistoryRepository
        )
        webSocketsService.start()
    }
}
