package com.ckgod.infrastructure.kis

import com.ckgod.config.KisConfig
import com.ckgod.infrastructure.kis.api.spec.KisApiSpec
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class KisApiClient(
    private val config: KisConfig,
    private val authService: KisAuthService,
    private val client: HttpClient
) {
    suspend fun request(
        spec: KisApiSpec,
        queryParams: Map<String, String> = emptyMap(),
        bodyParams: Map<String, String> = emptyMap()
    ): JsonObject {
        val token = authService.getAccessToken()
        val url = "${config.baseUrl}${spec.path}"
        val trId = spec.getTrId(config.mode)

        val response = when (spec.method) {
            HttpMethod.Get -> client.get(url) {
                headers { applyKisHeaders(token, trId) }
                url { queryParams.forEach { (key, value) -> parameters.append(key, value) } }
            }
            HttpMethod.Post -> client.post(url) {
                headers { applyKisHeaders(token, trId) }
                contentType(ContentType.Application.Json)
                setBody(bodyParams)
            }
            else -> throw KisApiException("Unsupported HTTP method: ${spec.method}")
        }

        return parseResponse(response)
    }

    suspend fun get(
        path: String,
        trId: String,
        configure: HttpRequestBuilder.() -> Unit = {}
    ): JsonObject {
        val token = authService.getAccessToken()

        val response = client.get("${config.baseUrl}$path") {
            headers { applyKisHeaders(token, trId) }
            configure()
        }

        return parseResponse(response)
    }

    suspend fun post(
        path: String,
        trId: String,
        body: Map<String, String> = emptyMap(),
        configure: HttpRequestBuilder.() -> Unit = {}
    ): JsonObject {
        val token = authService.getAccessToken()

        val response = client.post("${config.baseUrl}$path") {
            headers { applyKisHeaders(token, trId) }
            contentType(ContentType.Application.Json)
            setBody(body)
            configure()
        }

        return parseResponse(response)
    }

    private fun HeadersBuilder.applyKisHeaders(token: String, trId: String) {
        append("content-type", "application/json; charset=utf-8")
        append("authorization", "Bearer $token")
        append("appkey", config.appKey)
        append("appsecret", config.appSecret)
        append("custtype", "P")
        append("tr_id", trId)
    }

    private suspend fun parseResponse(response: HttpResponse): JsonObject {
        val fullResponse = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        return fullResponse["output"]?.jsonObject ?: JsonObject(emptyMap())
    }
}

class KisApiException(message: String) : RuntimeException(message)
