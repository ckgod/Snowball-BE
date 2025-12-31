package com.ckgod.kis.websokets

import com.ckgod.kis.auth.KisAuthService
import com.ckgod.kis.config.KisConfig
import io.ktor.client.HttpClient

class KisWebSocketsService(
    private val config: KisConfig,
    private val authService: KisAuthService,
    private val httpClient: HttpClient,
    private val processor: ExecutionNotificationProcessor
) {

    private val client = KisWebSocketClient(
        config = config,
        authService = authService,
        httpClient = httpClient,
        onNotificationReceived = { notification ->
            processor.process(notification)
        }
    )

    fun start() {
        processor.start()
        client.start()
    }

    fun stop() {
        client.stop()
        processor.stop()
    }
}
