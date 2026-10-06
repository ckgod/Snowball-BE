package com.ckgod.presentation.config

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs

/**
 * 실제 주문을 만들거나 바꾸는 요청(신규·정정·취소)만 지키는 추가 관문.
 *
 * - 주문 키(ORDER_API_KEY)는 네트워크로 보내지 않고 서명에만 쓴다.
 *   X-Order-Timestamp(초) 와 X-Order-Signature = hex(HMAC-SHA256(key, "ts\nMETHOD\npath\nbody")) 를 검증한다.
 * - 60초가 지난 서명, 이미 쓴 서명은 거절한다. 가로챈 요청을 그대로 다시 보내도 통하지 않는다.
 * - Idempotency-Key 가 같으면 한 번만 처리하고 처음 결과를 돌려준다 (재시도·연타 대비).
 * - 조회보다 훨씬 좁은 요청 한도를 둔다. ORDER_API_KEY 가 없는 서버에서는 주문 API 를 닫는다.
 */
class OrderGuard(
    private val orderKey: String?,
    private val maxRequests: Int = 10,
    private val windowMillis: Long = 60_000,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val requestTimes = ArrayDeque<Long>()
    private val usedSignatures = LinkedHashMap<String, Long>()
    private val idempotentResults = LinkedHashMap<String, Pair<Long, Any>>()

    val enabled: Boolean get() = !orderKey.isNullOrBlank()

    /** 통과하면 true. 막으면 응답까지 보내고 false. */
    suspend fun check(context: RoutingContext, body: String): Boolean {
        val call = context.call
        val rejection = verify(
            method = call.request.httpMethod.value,
            path = call.request.path(),
            body = body,
            timestamp = call.request.headers["X-Order-Timestamp"],
            signature = call.request.headers["X-Order-Signature"]
        ) ?: return true
        call.respond(rejection.first, mapOf("error" to rejection.second))
        return false
    }

    /** 실패 사유를 돌려준다. 통과면 null. 라우팅과 분리해 단위 테스트한다. */
    @Synchronized
    fun verify(method: String, path: String, body: String, timestamp: String?, signature: String?): Pair<HttpStatusCode, String>? {
        if (!enabled) return HttpStatusCode.ServiceUnavailable to "주문 API 가 비활성화되어 있습니다 (ORDER_API_KEY 미설정)"

        val now = clock()
        val ts = timestamp?.toLongOrNull()
        if (ts == null || signature.isNullOrBlank()) return HttpStatusCode.Forbidden to "주문 서명이 없습니다"
        if (abs(now / 1000 - ts) > MAX_SKEW_SECONDS) return HttpStatusCode.Forbidden to "주문 서명이 만료됐습니다 (기기 시간을 확인해 주세요)"

        val expected = sign(orderKey!!, ts, method, path, body)
        if (!MessageDigest.isEqual(expected.toByteArray(), signature.lowercase().toByteArray())) {
            return HttpStatusCode.Forbidden to "주문 서명이 유효하지 않습니다"
        }

        usedSignatures.entries.removeIf { now - it.value > REPLAY_WINDOW_MILLIS }
        if (usedSignatures.containsKey(signature)) return HttpStatusCode.Forbidden to "이미 처리된 요청입니다"

        while (requestTimes.isNotEmpty() && now - requestTimes.first() > windowMillis) requestTimes.removeFirst()
        if (requestTimes.size >= maxRequests) return HttpStatusCode.TooManyRequests to "주문 요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요."

        usedSignatures[signature] = now
        requestTimes.addLast(now)
        return null
    }

    /**
     * 같은 Idempotency-Key 로 다시 오면 block 을 실행하지 않고 처음 결과를 돌려준다.
     */
    suspend fun <T : Any> idempotent(key: String?, block: suspend () -> T): T {
        if (key.isNullOrBlank()) return block()
        cached<T>(key)?.let { return it }
        val result = block()
        synchronized(this) { idempotentResults[key] = clock() to result }
        return result
    }

    @Synchronized
    @Suppress("UNCHECKED_CAST")
    private fun <T> cached(key: String): T? {
        val now = clock()
        idempotentResults.entries.removeIf { now - it.value.first > IDEMPOTENCY_TTL_MILLIS }
        return idempotentResults[key]?.second as T?
    }

    companion object {
        private const val MAX_SKEW_SECONDS = 60L
        private const val REPLAY_WINDOW_MILLIS = 2 * 60_000L
        private const val IDEMPOTENCY_TTL_MILLIS = 10 * 60_000L

        /** 앱과 같은 규칙. 바꾸면 앱 OrderSigner 도 같이 바꿔야 한다. */
        fun sign(key: String, timestamp: Long, method: String, path: String, body: String): String {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(key.toByteArray(), "HmacSHA256"))
            val payload = "$timestamp\n${method.uppercase()}\n$path\n$body"
            return mac.doFinal(payload.toByteArray()).joinToString("") { "%02x".format(it) }
        }
    }
}
