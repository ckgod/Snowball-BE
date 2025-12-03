package com.ckgod.infrastructure.kis

import com.ckgod.config.KisConfig
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class KisApiClient(
    private val config: KisConfig,
    private val authService: KisAuthService,
    private val client: HttpClient
) {
    suspend fun get(
        path: String,
        trId: String,
        configure: HttpRequestBuilder.() -> Unit = {}
    ): JsonObject {
        val token = authService.getAccessToken()

        val response = client.get("${config.baseUrl}$path") {
            headers {
                append("content-type", "application/json; charset=utf-8")
                append("authorization", "Bearer $token")
                append("appkey", config.appKey)
                append("appsecret", config.appSecret)
                append("custtype", "P")
                append("tr_id", trId)
            }
            configure()
        }

        val fullResponse = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        return fullResponse["output"]?.jsonObject ?: JsonObject(emptyMap())
    }
}

class KisApiException(message: String) : RuntimeException(message)
