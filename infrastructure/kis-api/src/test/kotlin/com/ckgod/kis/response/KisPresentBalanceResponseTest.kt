package com.ckgod.kis.response

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class KisPresentBalanceResponseTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun item(pdno: String, qty: String, settledQty: String, pchs: String, evlu: String, pfls: String) = """
        {"pdno":"$pdno","prdt_name":"$pdno","cblc_qty13":"$settledQty","thdt_buy_ccld_qty1":"0","thdt_sll_ccld_qty1":"0",
         "ccld_qty_smtl1":"$qty","ord_psbl_qty1":"$qty","frcr_pchs_amt":"$pchs","frcr_evlu_amt2":"$evlu",
         "evlu_pfls_amt2":"$pfls","evlu_pfls_rt1":"-10.00","bass_exrt":"1400.0","buy_crcy_cd":"USD",
         "ovrs_now_pric1":"50.0","avg_unpr3":"55.0","tr_mket_name":"","natn_kor_name":"","pchs_rmnd_wcrc_amt":"",
         "thdt_buy_ccld_frcr_amt":"","thdt_sll_ccld_frcr_amt":"","unit_amt":"","std_pdno":"","prdt_type_cd":"",
         "scts_dvsn_name":"","loan_rmnd":"","loan_dt":"","loan_expd_dt":"","ovrs_excg_cd":"AMEX","item_lnkg_excg_cd":""}
    """.trimIndent()

    private fun body(items: List<String>, deposit: String, buy: String, sell: String) = """
        {"rt_cd":"0","msg_cd":"","msg1":"",
         "output1":[${items.joinToString(",")}],
         "output2":[{"crcy_cd":"USD","crcy_cd_name":"","frcr_buy_amt_smtl":"$buy","frcr_sll_amt_smtl":"$sell",
                     "frcr_dncl_amt_2":"$deposit","frst_bltn_exrt":"1400.0","frcr_buy_mgn_amt":"$buy","frcr_etc_mgna":"0",
                     "frcr_drwg_psbl_amt_1":"0","frcr_evlu_amt2":"0","acpl_cstd_crcy_yn":"N","nxdy_frcr_drwg_psbl_amt":"0"}],
         "output3":{"pchs_amt_smtl":"1100","evlu_amt_smtl":"1000","evlu_pfls_amt_smtl":"-100","dncl_amt":"0",
                    "cma_evlu_amt":"0","tot_dncl_amt":"0","etc_mgna":"0","wdrw_psbl_tot_amt":"0","frcr_evlu_tota":"0",
                    "evlu_erng_rt1":"-9.09","pchs_amt_smtl_amt":"0","evlu_amt_smtl_amt":"0","tot_evlu_pfls_amt":"0",
                    "tot_asst_amt":"0","buy_mgn_amt":"0","mgna_tota":"0","frcr_use_psbl_amt":"0",
                    "ustl_sll_amt_smtl":"0","ustl_buy_amt_smtl":"0","tot_frcr_cblc_smtl":"0","tot_loan_amt":"0"}}
    """.trimIndent()

    @Test
    fun `현금은 결제 전 매수 대금을 빼고 매도 대금을 더한 체결 기준이다`() {
        val status = json.decodeFromString<KisPresentBalanceResponse>(
            body(listOf(item("SOXL", "20", "18", "1100.40", "1000.00", "-100.40")), deposit = "500.00", buy = "100.00", sell = "30.00")
        ).toDomain()

        assertEquals(430.0, status.orderableCashUsd)
        assertEquals(500.0, status.totalCashUsd)
        assertEquals(1430.0, status.totalAssetValueUsd)
    }

    @Test
    fun `주식 합계는 잘려 오는 output3 대신 종목별 소수점 값을 더한다`() {
        val status = json.decodeFromString<KisPresentBalanceResponse>(
            body(
                listOf(
                    item("SOXL", "20", "20", "1100.40", "1000.25", "-100.15"),
                    item("TQQQ", "3", "3", "200.10", "210.50", "10.40"),
                ),
                deposit = "0", buy = "0", sell = "0"
            )
        ).toDomain()

        assertEquals(1300.5, status.totalBuyingValueUsd)
        assertEquals(1210.75, status.totalEvalValueUsd)
        assertEquals(-89.75, status.totalProfitUsd)
    }
}
