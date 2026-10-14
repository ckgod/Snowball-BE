package com.ckgod.kis.response

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KisExecutionResponseTest {

    private val json = Json { ignoreUnknownKeys = true }

    /** 2026-10-14 실제 응답에서 계좌·금액 외 필드만 남긴 것 */
    private val body = """
        {"rt_cd":"0","msg_cd":"","msg1":"","ctx_area_fk200":"","ctx_area_nk200":"",
         "output":[
          {"ord_dt":"20261014","odno":"0030605916","orgn_odno":"0030591307","sll_buy_dvsn_cd":"02","rvse_cncl_dvsn":"01",
           "pdno":"SOXL","prdt_name":"SOXL","prcs_stat_name":"거부","ft_ord_qty":"1","ft_ord_unpr3":"164.05000000",
           "ft_ccld_qty":"0","ft_ccld_unpr3":"0.00000000","ft_ccld_amt3":"0.00000","nccs_qty":"0",
           "rjct_rson_name":"LimitPrice {164.05 > 30.24% Limit 30.00% thru nbbo } nbbo:12"},
          {"ord_dt":"20261014","odno":"0030591307","orgn_odno":"","sll_buy_dvsn_cd":"02","rvse_cncl_dvsn":"",
           "pdno":"SOXL","prdt_name":"SOXL","prcs_stat_name":"전송","ft_ord_qty":"1","ft_ord_unpr3":"108.43000000",
           "ft_ccld_qty":"0","ft_ccld_unpr3":"0.00000000","ft_ccld_amt3":"0.00000","nccs_qty":"1","rjct_rson_name":""}
         ]}
    """.trimIndent()

    @Test
    fun `처리상태가 거부면 사유를 담는다`() {
        val (rejected, alive) = json.decodeFromString<KisExecutionResponse>(body).output.map { it.toDomain() }

        assertEquals("LimitPrice {164.05 > 30.24% Limit 30.00% thru nbbo } nbbo:12", rejected.rejectReason)
        assertNull(alive.rejectReason)
    }
}
