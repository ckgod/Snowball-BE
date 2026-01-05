package com.ckgod.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import kotlinx.serialization.json.Json
import org.koin.dsl.module


val httpClientModule = module {
    single {
        val jsonConfig = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        HttpClient(CIO) {
            val config = get<Application>().environment.config
            if (config.property("app.environment").getString() == "local") {
                install(Logging) {
                    logger = Logger.SIMPLE
                    level = LogLevel.ALL
                }
            }
            install(ContentNegotiation) {
                json(jsonConfig)
            }
        }
    }
}
