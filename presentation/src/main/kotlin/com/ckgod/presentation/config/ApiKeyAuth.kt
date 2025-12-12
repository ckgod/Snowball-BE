package com.ckgod.presentation.config

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.AttributeKey

fun Application.configureAuthPlugin(apiKey: String) {
    install(ApiKeyAuthPlugin) {
        this.apiKey = apiKey
    }
}

class ApiKeyAuthPlugin(private val apiKey: String) {

    class Config {
        var apiKey: String = ""
    }

    companion object Plugin : BaseApplicationPlugin<ApplicationCallPipeline, Config, ApiKeyAuthPlugin> {
        override val key = AttributeKey<ApiKeyAuthPlugin>("ApiKeyAuth")

        override fun install(
            pipeline: ApplicationCallPipeline,
            configure: Config.() -> Unit
        ): ApiKeyAuthPlugin {
            val config = Config().apply(configure)
            val plugin = ApiKeyAuthPlugin(config.apiKey)

            pipeline.intercept(ApplicationCallPipeline.Plugins) {
                val requestApiKey = call.request.headers["X-API-Key"]

                if (requestApiKey == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "API Key가 필요합니다"))
                    finish()
                    return@intercept
                }

                if (requestApiKey != plugin.apiKey) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "유효하지 않은 API Key입니다"))
                    finish()
                    return@intercept
                }
            }

            return plugin
        }
    }
}
