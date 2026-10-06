package com.ckgod.presentation.config

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OrderGuardTest {

    private val key = "test-order-key"
    private var now = 1_791_000_000_000L
    private fun guard(maxRequests: Int = 10) = OrderGuard(orderKey = key, maxRequests = maxRequests, clock = { now })

    private fun signed(guard: OrderGuard, body: String = "{}", path: String = "/sb/orders", ts: Long = now / 1000): Pair<HttpStatusCode, String>? {
        val signature = OrderGuard.sign(key, ts, "POST", path, body)
        return guard.verify("POST", path, body, ts.toString(), signature)
    }

    @Test
    fun `올바른 서명은 통과한다`() {
        assertNull(signed(guard()))
    }

    @Test
    fun `본문이 바뀌면 서명이 맞지 않는다`() {
        val g = guard()
        val ts = now / 1000
        val signature = OrderGuard.sign(key, ts, "POST", "/sb/orders", """{"quantity":1}""")
        val result = g.verify("POST", "/sb/orders", """{"quantity":100}""", ts.toString(), signature)
        assertEquals(HttpStatusCode.Forbidden, result?.first)
    }

    @Test
    fun `60초가 지난 서명은 거절한다`() {
        assertEquals(HttpStatusCode.Forbidden, signed(guard(), ts = now / 1000 - 61)?.first)
    }

    @Test
    fun `같은 서명을 다시 보내면 거절한다`() {
        val g = guard()
        val ts = now / 1000
        val signature = OrderGuard.sign(key, ts, "POST", "/sb/orders", "{}")
        assertNull(g.verify("POST", "/sb/orders", "{}", ts.toString(), signature))
        assertEquals(HttpStatusCode.Forbidden, g.verify("POST", "/sb/orders", "{}", ts.toString(), signature)?.first)
    }

    @Test
    fun `서명이 없으면 거절한다`() {
        assertEquals(HttpStatusCode.Forbidden, guard().verify("POST", "/sb/orders", "{}", null, null)?.first)
    }

    @Test
    fun `키가 없는 서버는 503`() {
        val g = OrderGuard(orderKey = null)
        assertEquals(HttpStatusCode.ServiceUnavailable, g.verify("POST", "/sb/orders", "{}", "1", "x")?.first)
    }

    @Test
    fun `한도를 넘으면 429`() {
        val g = guard(maxRequests = 2)
        assertNull(signed(g, body = "1"))
        assertNull(signed(g, body = "2"))
        assertEquals(HttpStatusCode.TooManyRequests, signed(g, body = "3")?.first)
    }

    @Test
    fun `같은 멱등 키는 한 번만 실행한다`() = runBlocking {
        val g = guard()
        var calls = 0
        val first = g.idempotent("k1") { calls++; "result-$calls" }
        val second = g.idempotent("k1") { calls++; "result-$calls" }
        assertEquals(1, calls)
        assertEquals(first, second)
    }

    /** 앱 OrderSignerTest 와 같은 벡터. 한쪽 규칙만 바뀌면 둘 중 하나가 깨진다. */
    @Test
    fun `앱과 같은 서명을 만든다`() {
        val signature = OrderGuard.sign("test-key", 1_700_000_000, "post", "/sb/orders", """{"ticker":"TQQQ"}""")
        assertEquals("8bf571b847039a9eea44b43cb0007f65398eb3da753996d422c4f59fe98fce65", signature)
    }
}
