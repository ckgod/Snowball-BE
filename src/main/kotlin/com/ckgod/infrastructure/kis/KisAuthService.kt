package com.ckgod.infrastructure.kis

import com.ckgod.config.KisConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class KisAuthService(
    private val config: KisConfig,
    private val client: HttpClient
) {
    private var accessToken: String? = null
    private var tokenExpiryTime: Long = 0

    private val mutex = Mutex()

    suspend fun getAccessToken(): String {
        if (isValidToken()) return accessToken!!

        return mutex.withLock {
            if (isValidToken()) return@withLock accessToken!!

            refreshToken()
        }
    }

    private fun isValidToken(): Boolean {
        val currentTime = System.currentTimeMillis()

        return accessToken != null && currentTime < tokenExpiryTime - 60_000
    }

    private suspend fun refreshToken(): String {
        val response = client.post("${config.baseUrl}/oauth2/tokenP") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "grant_type" to "client_credentials",
                "appkey" to config.appKey,
                "appsecret" to config.appSecret
            ))
        }

        val jsonBody = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        accessToken = jsonBody["access_token"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("token api error: ${jsonBody["msg1"]?.jsonPrimitive?.content}")

        val expiredStr = jsonBody["access_token_token_expired"]?.jsonPrimitive?.content
        tokenExpiryTime = if (expiredStr != null) {
            try {
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                val expiredDateTime = LocalDateTime.parse(expiredStr, formatter)
                expiredDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (_: Exception) {
                println("만료 시간 파싱 실패: $expiredStr, 기본값(24시간) 사용")
                System.currentTimeMillis() + (86400 * 1000)
            }
        } else {
            System.currentTimeMillis() + (86400 * 1000)
        }

        val remainingSeconds = (tokenExpiryTime - System.currentTimeMillis()) / 1000
        println("success refresh token (expires at: $expiredStr, remaining: ${remainingSeconds}s)")
        println("token: $accessToken")
        return accessToken!!
    }
}
