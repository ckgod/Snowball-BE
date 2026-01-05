package com.ckgod.kis

import com.ckgod.kis.auth.KisAuthService
import com.ckgod.kis.config.KisConfig
import com.ckgod.kis.spec.KisApiSpec
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json

class KisApiClient(
    val config: KisConfig,
    private val authService: KisAuthService,
    private val client: HttpClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    internal suspend inline fun <reified T, reified Body> request(
        spec: KisApiSpec,
        queryParams: Map<String, String> = emptyMap(),
        bodyParams: Body? = null,
        additionalHeaders: Map<String, String>? = null
    ): T {
        val token = authService.getAccessToken()
        val url = "${config.baseUrl}${spec.path}"
        val trId = spec.getTrId(config.mode)

        val response = when (spec.method) {
            HttpMethod.Get -> client.get(url) {
                headers { applyKisHeaders(token, trId, additionalHeaders) }
                url { queryParams.forEach { (key, value) -> parameters.append(key, value) } }
            }
            HttpMethod.Post -> client.post(url) {
                headers { applyKisHeaders(token, trId, additionalHeaders) }
                contentType(ContentType.Application.Json)
                setBody(bodyParams)
            }
            else -> throw KisApiException("Unsupported HTTP method: ${spec.method}")
        }

        return json.decodeFromString<T>(response.bodyAsText())
    }

    internal suspend inline fun <reified T, reified Body> requestWithHeaders(
        spec: KisApiSpec,
        queryParams: Map<String, String> = emptyMap(),
        bodyParams: Body? = null,
        additionalHeaders: Map<String, String>? = null
    ): KisResponseWithHeaders<T> {
        val token = authService.getAccessToken()
        val url = "${config.baseUrl}${spec.path}"
        val trId = spec.getTrId(config.mode)

        val response = when (spec.method) {
            HttpMethod.Get -> client.get(url) {
                headers { applyKisHeaders(token, trId, additionalHeaders) }
                url { queryParams.forEach { (key, value) -> parameters.append(key, value) } }
            }
            HttpMethod.Post -> client.post(url) {
                headers { applyKisHeaders(token, trId, additionalHeaders) }
                contentType(ContentType.Application.Json)
                setBody(bodyParams)
            }
            else -> throw KisApiException("Unsupported HTTP method: ${spec.method}")
        }

        val body = json.decodeFromString<T>(response.bodyAsText())
        val headers = response.headers.entries()
            .associate { (key, values) -> key to values.firstOrNull().orEmpty() }

        return KisResponseWithHeaders(body, headers)
    }

    private fun HeadersBuilder.applyKisHeaders(token: String, trId: String, additionalHeaders: Map<String, String>? = null) {
        append("content-type", "application/json; charset=utf-8")
        append("authorization", "Bearer $token")
        append("appkey", config.appKey)
        append("appsecret", config.appSecret)
        append("custtype", "P")
        append("tr_id", trId)
        additionalHeaders?.forEach { key, value ->
            append(key, value)
        }
    }
}

class KisApiException(message: String) : RuntimeException(message)

data class KisResponseWithHeaders<T>(
    val body: T,
    val headers: Map<String, String>
)
