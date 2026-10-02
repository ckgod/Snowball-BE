package com.ckgod.presentation.config

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import java.security.MessageDigest

/**
 * 실제 주문을 바꾸는 요청(정정·취소)만 지키는 추가 관문.
 *
 * - 조회용 X-API-Key 와 별도로 X-Order-Key 를 요구한다. 조회 키가 새도 주문은 못 건드린다.
 * - 조회보다 훨씬 좁은 요청 한도를 둔다.
 * - ORDER_API_KEY 가 설정되지 않은 서버에서는 주문 API 를 아예 닫는다.
 */
class OrderGuard(
    private val orderKey: String?,
    private val maxRequests: Int = 10,
    private val windowMillis: Long = 60_000
) {
    private val requestTimes = ArrayDeque<Long>()

    val enabled: Boolean get() = !orderKey.isNullOrBlank()

    /** 통과하면 true. 막으면 응답까지 보내고 false. */
    suspend fun check(context: RoutingContext): Boolean {
        val call = context.call
        if (!enabled) {
            call.respond(HttpStatusCode.ServiceUnavailable, mapOf("error" to "주문 API 가 비활성화되어 있습니다 (ORDER_API_KEY 미설정)"))
            return false
        }
        val given = call.request.headers["X-Order-Key"]
        if (given == null || !constantTimeEquals(given, orderKey!!)) {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "주문 키가 유효하지 않습니다"))
            return false
        }
        if (!allow()) {
            call.respond(HttpStatusCode.TooManyRequests, mapOf("error" to "주문 요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요."))
            return false
        }
        return true
    }

    @Synchronized
    private fun allow(): Boolean {
        val now = System.currentTimeMillis()
        while (requestTimes.isNotEmpty() && now - requestTimes.first() > windowMillis) requestTimes.removeFirst()
        if (requestTimes.size >= maxRequests) return false
        requestTimes.addLast(now)
        return true
    }

    private fun constantTimeEquals(a: String, b: String): Boolean =
        MessageDigest.isEqual(a.toByteArray(), b.toByteArray())
}
