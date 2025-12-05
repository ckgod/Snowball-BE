package com.ckgod

import com.ckgod.config.KisConfig
import com.ckgod.config.KisMode
import com.ckgod.database.DatabaseFactory
import com.ckgod.database.auth.AuthTokenRepositoryImpl
import com.ckgod.service.StockService
import com.ckgod.infrastructure.kis.KisApiClient
import com.ckgod.infrastructure.kis.KisAuthService
import com.ckgod.infrastructure.kis.api.KisStockApi
import io.ktor.client.*
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.serialization.json.Json

fun main(args: Array<String>) {
    EngineMain.main(args)
}

@Suppress("unused")
fun Application.mainModule() {
    module()
}

fun Application.module() {
    DatabaseFactory.init()

    val config = environment.config

    val httpClient = HttpClient(CIO) {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    val realConfig = KisConfig(
        mode = KisMode.REAL,
        baseUrl = config.property("kis.real.baseUrl").getString().trim(),
        appKey = config.property("kis.real.appKey").getString().trim(),
        appSecret = config.property("kis.real.appSecret").getString().trim(),
        accountNo = config.property("kis.real.accountNo").getString().trim(),
    )

    val mockConfig = KisConfig(
        mode = KisMode.MOCK,
        baseUrl = config.property("kis.mock.baseUrl").getString().trim(),
        appKey = config.property("kis.mock.appKey").getString().trim(),
        appSecret = config.property("kis.mock.appSecret").getString().trim(),
        accountNo = config.property("kis.mock.accountNo").getString().trim(),
    )

    val authTokenRepository = AuthTokenRepositoryImpl()

    val realAuthService = KisAuthService(realConfig, httpClient, authTokenRepository)
    val mockAuthService = KisAuthService(mockConfig, httpClient, authTokenRepository)

    val realApiClient = KisApiClient(realConfig, realAuthService, httpClient)
    val mockApiClient = KisApiClient(mockConfig, mockAuthService, httpClient)

    val realStockApi = KisStockApi(realApiClient)
    val mockStockApi = KisStockApi(mockApiClient)

    val stockService = StockService(realStockApi, mockStockApi)

    configureSerialization()
    configureRouting(stockService)
}
