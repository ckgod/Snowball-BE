package com.ckgod.presentation.config

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.AttributeKey
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

fun Application.configureRateLimiter(maxRequests: Int, windowSeconds: Long) {
    install(RateLimiterPlugin) {
        this.maxRequests = maxRequests
        this.windowSeconds = windowSeconds
    }
}

class RateLimiterPlugin(
    private val maxRequests: Int,
    private val windowSeconds: Long
) {
    private val requestLog = ConcurrentHashMap<String, MutableList<Long>>()
    private val mutex = Mutex()

    class Config {
        var maxRequests: Int = 100 // 기본값: 100회
        var windowSeconds: Long = 60 // 기본값: 60초
    }

    companion object Plugin : BaseApplicationPlugin<ApplicationCallPipeline, Config, RateLimiterPlugin> {
        override val key = AttributeKey<RateLimiterPlugin>("RateLimiter")

        override fun install(
            pipeline: ApplicationCallPipeline,
            configure: Config.() -> Unit
        ): RateLimiterPlugin {
            val config = Config().apply(configure)
            val plugin = RateLimiterPlugin(config.maxRequests, config.windowSeconds)

            pipeline.intercept(ApplicationCallPipeline.Plugins) {
                val clientId = call.request.headers["X-API-Key"] ?: "anonymous"

                if (!plugin.allowRequest(clientId)) {
                    call.respond(
                        HttpStatusCode.TooManyRequests,
                        mapOf("error" to "요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.")
                    )
                    finish()
                    return@intercept
                }
            }

            return plugin
        }
    }

    private suspend fun allowRequest(clientId: String): Boolean = mutex.withLock {
        val now = Instant.now().epochSecond
        val timestamps = requestLog.computeIfAbsent(clientId) { mutableListOf() }

        timestamps.removeIf { it < now - windowSeconds }

        if (timestamps.size >= maxRequests) {
            return false
        }

        timestamps.add(now)
        return true
    }
}
