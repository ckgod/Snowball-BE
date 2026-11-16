package com.ckgod.kis.response

import com.ckgod.kis.KisApiException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KisBalanceResponseTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `조회 실패 응답은 보유 종목 없음이 아니라 예외다`() {
        val body = """{"rt_cd":"1","msg_cd":"EGW00201","msg1":"초당 거래건수를 초과하였습니다."}"""

        val error = assertFailsWith<KisApiException> {
            json.decodeFromString<KisBalanceResponse>(body).toDomain()
        }
        assertTrue(error.message!!.contains("EGW00201"))
    }

    @Test
    fun `실패 응답에 빈 보유 목록이 같이 와도 예외다`() {
        val body = """{"rt_cd":"1","msg_cd":"OPSQ0002","msg1":"조회 오류","output1":[],"output2":null}"""

        assertFailsWith<KisApiException> {
            json.decodeFromString<KisBalanceResponse>(body).toDomain()
        }
    }

    @Test
    fun `성공 응답에 보유 종목이 없으면 빈 목록이다`() {
        val body = """{"rt_cd":"0","msg_cd":"KIOK0510","msg1":"조회가 완료되었습니다","output1":[],"output2":null}"""

        val account = json.decodeFromString<KisBalanceResponse>(body).toDomain()

        assertEquals(emptyList(), account.holdings)
    }

    @Test
    fun `보유 종목을 도메인으로 변환한다`() {
        val body = """
            {"rt_cd":"0","msg_cd":"","msg1":"",
             "output1":[{"ovrs_pdno":"SOXL","ovrs_item_name":"DIREXION SEMICONDUCTOR DAILY 3X","ovrs_cblc_qty":"125",
                         "pchs_avg_pric":"85.9906","frcr_pchs_amt1":"10748.83","ovrs_stck_evlu_amt":"6885",
                         "frcr_evlu_pfls_amt":"-3863.83","evlu_pfls_rt":"-35.95","now_pric2":"55.08"}],
             "output2":null}
        """.trimIndent()

        val holding = json.decodeFromString<KisBalanceResponse>(body).toDomain().holdings.single()

        assertEquals("SOXL", holding.ticker)
        assertEquals("125", holding.quantity)
        assertEquals("85.9906", holding.avgPrice)
        assertEquals("10748.83", holding.investedAmount)
    }
}
