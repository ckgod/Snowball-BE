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
import kotlin.time.Duration.Companion.seconds

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

    private var aesKey: String? = null
    private var aesIv: String? = null

    fun start() {
        if (job?.isActive == true) return

        job = scope.launch {
            while (isActive) {
                try {
                    connectAndListen()
                } catch (e: Exception) {
                    logger.error("웹소켓 예외 발생: ${e.message}")
                } finally {
                    delay(5.seconds)
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
                        val rawMessage = frame.readText()
                        if (!rawMessage.contains("PINGPONG")) {
                            try {
                                if (rawMessage.startsWith("{")) {
                                    handleJsonResponse(rawMessage)
                                } else {
                                    handleRealtimeData(rawMessage)
                                }
                            } catch (e: Exception) {
                                logger.error("메시지 처리 중 오류 발생: ${e.message}", e)
                            }
                        }
                    }
                    is Frame.Ping -> send(Frame.Pong(frame.data))
                    else -> Unit
                }
            }
        }
        logger.info("웹소켓 세션이 종료되었습니다.")
    }

    private fun handleJsonResponse(jsonMessage: String) {
        val response = jsonConverter.decodeFromString<KisWsResponse>(jsonMessage)

        if (response.body.output != null) {
            aesKey = response.body.output.key
            aesIv = response.body.output.iv
            logger.info("AES 암호화 키 설정 완료 - msg: ${response.body.msg1}")
        }
    }

    private fun handleRealtimeData(data: String) {
        val parts = data.split("|")
        if (parts.size < 4) {
            logger.warn("잘못된 실시간 데이터 형식: $data")
            return
        }

        val isEncrypted = parts[0] == "1"
        val trId = parts[1]
        val dataCount = parts[2]
        val payload = parts[3]

        logger.info("실시간 데이터 수신 - TR_ID: $trId, 데이터건수: $dataCount, 암호화: $isEncrypted")

        val decryptedPayload = if (isEncrypted) {
            if (aesKey == null || aesIv == null) {
                logger.error("AES 키가 설정되지 않았습니다. 암호화된 데이터를 복호화할 수 없습니다.")
                return
            }
            try {
                AesCrypto.decrypt(aesKey!!, aesIv!!, payload)
            } catch (e: Exception) {
                logger.error("복호화 실패: ${e.message}", e)
                return
            }
        } else {
            payload
        }

        val fields = decryptedPayload.split("^")

        try {
            val notification = RealtimeExecutionNotification.from(fields)
            logger.info("실시간 체결 통보 수신: 종목코드=${notification.stockShortCode}, " +
                    "주문종류=${notification.orderType2}, " +
                    "체결여부=${notification.executionFlag}, " +
                    "체결수량=${notification.executionQuantity}, " +
                    "체결단가=${notification.executionPrice}, " +
                    "매도매수=${notification.sellBuyType}, " +
                    "체결시간=${notification.stockExecutionTime}")

        } catch (e: IllegalArgumentException) {
            logger.error("실시간 데이터 파싱 실패: ${e.message}")
            logger.debug("원본 데이터: $decryptedPayload")
        }
    }
}