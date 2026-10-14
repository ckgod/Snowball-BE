package com.ckgod.domain.usecase

import com.ckgod.domain.repository.ExecutionRepository
import com.ckgod.domain.utils.today
import com.ckgod.domain.utils.yesterday
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory

/**
 * 접수된 주문이 직후 거래소에서 거부됐는지 확인한다.
 *
 * KIS 는 주문·정정 요청에 주문번호를 돌려준 뒤에도 거래소 단계에서 거부할 수 있다.
 * (2026-10-13~14 SOXL: LOC 매수가가 호가보다 30% 넘게 높아 `[6008] Limit Price Too Aggressive` 로 거부.
 *  주문번호를 받았다는 이유로 PENDING·정정 성공으로 기록해 이틀간 거부를 몰랐다)
 *
 * 거부는 보통 바로 오지만 늦게 오는 건은 여기서 못 잡는다. 그런 건은 새벽 정산이 REJECTED 로 고친다.
 */
class OrderRejectionChecker(
    private val executionRepository: ExecutionRepository,
    private val waitMillis: Long = 3_000
) {
    private val logger = LoggerFactory.getLogger(OrderRejectionChecker::class.java)

    /** @return 거부된 주문번호 → 사유. 확인에 실패하면 빈 맵 (접수로 본다) */
    suspend fun findRejected(orderNos: Collection<String>): Map<String, String> {
        val targets = orderNos.filter { it.isNotBlank() }.toSet()
        if (targets.isEmpty()) return emptyMap()

        delay(waitMillis)
        return try {
            // 자정 전후 주문도 잡도록 어제~오늘 주문일을 함께 본다
            executionRepository.getExecutionList(startDate = yesterday(), endDate = today())
                .filter { it.orderNo in targets }
                .mapNotNull { execution -> execution.rejectReason?.let { execution.orderNo to describeRejection(it) } }
                .toMap()
        } catch (e: Exception) {
            logger.warn("[OrderRejectionChecker] 거부 확인 실패 - 접수로 간주 (새벽 정산에서 다시 확인): ${e.message}")
            emptyMap()
        }
    }

    companion object {
        /** 거래소 원문 사유에 사람이 읽을 설명을 붙인다 */
        fun describeRejection(raw: String): String {
            val reason = raw.trim()
            val priceCollar = reason.contains("Too Aggressive", ignoreCase = true) || reason.contains("LimitPrice", ignoreCase = true)
            return if (priceCollar) "거래소 거부: 지정가가 현재 호가에서 30% 넘게 벗어남 ($reason)"
            else "거래소 거부: $reason"
        }
    }
}
