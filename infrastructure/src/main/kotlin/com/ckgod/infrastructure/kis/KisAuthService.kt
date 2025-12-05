package com.ckgod.infrastructure.kis

import com.ckgod.config.KisConfig
import com.ckgod.domain.auth.AuthTokenRepository
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
import java.time.format.DateTimeFormatter

class KisAuthService(
    private val config: KisConfig,
    private val client: HttpClient,
    private val authTokenRepository: AuthTokenRepository
) {
    private var accessToken: String? = null
    private var tokenExpiryTime: LocalDateTime? = null

    private val mutex = Mutex()

    init {
        val savedToken = authTokenRepository.getToken(config.mode.toString())
        if (savedToken != null && savedToken.expireAt.isAfter(LocalDateTime.now())) {
            accessToken = savedToken.accessToken
            tokenExpiryTime = savedToken.expireAt

            val remainingSeconds = java.time.Duration.between(LocalDateTime.now(), tokenExpiryTime).seconds
            println("[${config.mode}] DB에서 기존 토큰 로드 완료 (남은 시간: ${remainingSeconds}초)")
        } else if (savedToken != null) {
            println("[${config.mode}] DB에 저장된 토큰이 만료되었습니다. 새로 발급이 필요합니다.")
        }
    }

    suspend fun getAccessToken(): String {
        if (isValidToken()) return accessToken!!

        return mutex.withLock {
            if (isValidToken()) return@withLock accessToken!!

            refreshToken()
        }
    }

    private fun isValidToken(): Boolean {
        val expiryTime = tokenExpiryTime ?: return false
        val bufferTime = LocalDateTime.now().plusSeconds(60)

        return accessToken != null && bufferTime.isBefore(expiryTime)
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
        tokenExpiryTime = parseExpiryTime(expiredStr)

        val remainingSeconds = java.time.Duration.between(LocalDateTime.now(), tokenExpiryTime).seconds
        println("[${config.mode}] success refresh token (expires at: $expiredStr, remaining: ${remainingSeconds}s)")

        authTokenRepository.saveToken(config.mode.toString(), accessToken!!, tokenExpiryTime!!)
        println("[${config.mode}] 토큰이 DB에 저장되었습니다.")

        return accessToken!!
    }

    private fun parseExpiryTime(expiredStr: String?): LocalDateTime {
        return if (expiredStr != null) {
            try {
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                LocalDateTime.parse(expiredStr, formatter)
            } catch (_: Exception) {
                println("[${config.mode}] 만료 시간 파싱 실패: $expiredStr, 기본값(24시간) 사용")
                LocalDateTime.now().plusDays(1)
            }
        } else {
            LocalDateTime.now().plusDays(1)
        }
    }
}
