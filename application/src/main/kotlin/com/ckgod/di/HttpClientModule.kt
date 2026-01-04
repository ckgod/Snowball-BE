package com.ckgod.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import kotlin.time.Duration.Companion.seconds


val httpClientModule = module {
    single {
        val jsonConfig = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(jsonConfig)
            }
            install(WebSockets) {
                pingInterval = 20.seconds
                contentConverter = KotlinxWebsocketSerializationConverter(jsonConfig)
            }
        }
    }
}
