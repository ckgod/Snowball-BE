package com.ckgod.kis.response

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KisOrderResponseTest {

    // KisApiClient 와 같은 설정
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `접수 성공 응답은 주문번호를 담고 성공으로 판정된다`() {
        val body = """
            {"rt_cd":"0","msg_cd":"APBK0013","msg1":"주문 전송 완료 되었습니다.",
             "output":{"KRX_FWDG_ORD_ORGNO":"01790","ODNO":"0030138295","ORD_TMD":"180512"}}
        """.trimIndent()

        val response = json.decodeFromString<KisOrderResponse>(body)

        assertTrue(response.isSuccess)
        assertEquals("0030138295", response.output?.orderNo)
    }

    @Test
    fun `output 이 없는 거부 응답도 역직렬화되고 실패로 판정된다`() {
        val body = """{"rt_cd":"1","msg_cd":"APBK0952","msg1":"주문가능금액을 초과 했습니다"}"""

        val response = json.decodeFromString<KisOrderResponse>(body)

        assertFalse(response.isSuccess)
        assertNull(response.output)
        assertEquals("APBK0952", response.messageCode)
    }

    @Test
    fun `빈 output 이 오는 거부 응답도 실패로 판정된다`() {
        val body = """{"rt_cd":"1","msg_cd":"APBK1234","msg1":"거부","output":{}}"""

        val response = json.decodeFromString<KisOrderResponse>(body)

        assertFalse(response.isSuccess)
    }

    @Test
    fun `rt_cd 가 0 이어도 주문번호가 비어 있으면 실패로 판정된다`() {
        val body = """{"rt_cd":"0","msg_cd":"","msg1":"","output":{"ODNO":""}}"""

        val response = json.decodeFromString<KisOrderResponse>(body)

        assertFalse(response.isSuccess)
    }
}
