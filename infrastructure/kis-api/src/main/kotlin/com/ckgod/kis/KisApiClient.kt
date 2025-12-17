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
        bodyParams: Body? = null
    ): T {
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

        return json.decodeFromString<T>(response.bodyAsText())
    }

    private fun HeadersBuilder.applyKisHeaders(token: String, trId: String) {
        append("content-type", "application/json; charset=utf-8")
        append("authorization", "Bearer $token")
        append("appkey", config.appKey)
        append("appsecret", config.appSecret)
        append("custtype", "P")
        append("tr_id", trId)
    }
}

class KisApiException(message: String) : RuntimeException(message)
