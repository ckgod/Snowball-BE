package com.ckgod.kis.websokets

import com.ckgod.domain.repository.TradeHistoryRepository
import com.ckgod.kis.auth.KisAuthService
import com.ckgod.kis.config.KisConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.minutes

class KisWebSocketsService(
    private val config: KisConfig,
    private val authService: KisAuthService,
    private val httpClient: HttpClient,
    private val tradeHistoryRepository: TradeHistoryRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val wsHost = "ops.koreainvestment.com"
    private val wsPort = 21000

    private val jsonConverter = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun start() {
        if (job?.isActive == true) return

        job = scope.launch {
            while (isActive) {
                try {
                    connectAndListen()
                } catch (e: Exception) {
                    logger.error("웹소켓 예외 발생: ${e.message}")
                } finally {
                    delay(30.minutes)
                    logger.info("웹소켓 재연결 시도 ..")
                }
            }
        }
    }

    fun stop() {
        logger.info("KIS 실시간 체결 수신 서비스 종료 중...")
        job?.cancel()
        scope.cancel()
    }

    private suspend fun connectAndListen() {
        val approvalKey = authService.getApprovalKey()
        logger.info("웹소켓 접속 시도: ws://$wsHost:$wsPort")

        httpClient.webSocket(
            method = HttpMethod.Get,
            host = wsHost,
            port = wsPort,
            path = "/"
        ) {
            logger.info("웹소켓 연결 성공. 구독 요청 전송 중...")
            val request = KisWsRequest(
                header = KisWsRequest.Header(approval_key = approvalKey),
                body = KisWsRequest.Body(tr_key = config.userId),
            )
            send(jsonConverter.encodeToString(request))

            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        handleMessage(frame.readText())
                    }
                    is Frame.Ping -> send(Frame.Pong(frame.data))
                    else -> Unit
                }
            }
        }
        logger.info("웹소켓 세션이 종료되었습니다.")
    }

    private fun handleMessage(rawMessage: String) {
        if (rawMessage.contains("PINGPONG")) {
            return
        }
        logger.info("webSockets receive: ${rawMessage.trim()}")
    }
}