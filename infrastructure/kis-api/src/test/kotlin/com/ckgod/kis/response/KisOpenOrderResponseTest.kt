package com.ckgod.kis.response

import com.ckgod.domain.model.OrderSide
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class KisOpenOrderResponseTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `미체결 항목을 도메인으로 변환한다`() {
        val body = """
            {"rt_cd":"0","msg_cd":"","msg1":"","ctx_area_fk200":"","ctx_area_nk200":"",
             "output":[{"ord_dt":"20261002","odno":"0030138295","orgn_odno":"","pdno":"SOXL",
                        "sll_buy_dvsn_cd":"02","ord_tmd":"180512","ft_ord_qty":"5","ft_ccld_qty":"1",
                        "nccs_qty":"4","ft_ord_unpr3":"20.50000000","ovrs_excg_cd":"AMEX"}]}
        """.trimIndent()

        val order = json.decodeFromString<KisOpenOrderResponse>(body).output.single().toDomain()

        assertEquals("0030138295", order.orderNo)
        assertEquals(null, order.originalOrderNo)
        assertEquals(OrderSide.BUY, order.side)
        assertEquals(20.5, order.orderPrice)
        assertEquals(4, order.unfilledQuantity)
        assertEquals("AMEX", order.exchange)
    }

    @Test
    fun `sll_buy_dvsn_cd 01 은 매도다`() {
        val item = KisOpenOrderResponse.Item(orderNo = "1", sellBuyCode = "01")
        assertEquals(OrderSide.SELL, item.toDomain().side)
    }
}
