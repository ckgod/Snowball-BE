package com.ckgod.domain.usecase

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrderRejectionCheckerTest {

    @Test
    fun `요청한 주문번호의 거부만 돌려준다`() = runBlocking {
        val result = checker(mapOf("A" to "[6008] Limit Price Too Aggressive", "B" to "기타")).findRejected(listOf("A", "C"))

        assertEquals(setOf("A"), result.keys)
        assertTrue(result.getValue("A").contains("30% 넘게"))
    }

    @Test
    fun `가격 한도가 아닌 사유는 원문만 붙인다`() {
        assertEquals("거래소 거부: 기타", OrderRejectionChecker.describeRejection("기타"))
    }
}
