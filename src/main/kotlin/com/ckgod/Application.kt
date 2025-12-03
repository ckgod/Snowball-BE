package com.ckgod

import com.ckgod.config.KisConfig
import com.ckgod.domain.stock.StockService
import com.ckgod.infrastructure.kis.KisApiClient
import com.ckgod.infrastructure.kis.KisAuthService
import com.ckgod.infrastructure.kis.api.KisStockApi
import io.ktor.client.*
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    val config = environment.config

    val httpClient = HttpClient(OkHttp) {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    val realConfig = KisConfig(
        baseUrl = config.property("kis.real.baseUrl").getString().trim(),
        appKey = config.property("kis.real.appKey").getString().trim(),
        appSecret = config.property("kis.real.appSecret").getString().trim(),
        accountNo = config.property("kis.real.accountNo").getString().trim(),
    )

    val mockConfig = KisConfig(
        baseUrl = config.property("kis.mock.baseUrl").getString().trim(),
        appKey = config.property("kis.mock.appKey").getString().trim(),
        appSecret = config.property("kis.mock.appSecret").getString().trim(),
        accountNo = config.property("kis.mock.accountNo").getString().trim(),
    )

    val realAuthService = KisAuthService(realConfig, httpClient)
    val mockAuthService = KisAuthService(mockConfig, httpClient)

    val realApiClient = KisApiClient(realConfig, realAuthService, httpClient)
    val mockApiClient = KisApiClient(mockConfig, mockAuthService, httpClient)

    val realStockApi = KisStockApi(realApiClient)
    val mockStockApi = KisStockApi(mockApiClient)

    val stockService = StockService(realStockApi, mockStockApi)

    configureSerialization()
    configureRouting(stockService)
}
