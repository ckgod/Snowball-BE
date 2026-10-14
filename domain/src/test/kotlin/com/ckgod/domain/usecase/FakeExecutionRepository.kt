package com.ckgod.domain.usecase

import com.ckgod.domain.model.ExecutionInfo
import com.ckgod.domain.repository.ExecutionRepository

/** 주문번호 → 거래소 거부 사유. 비어 있으면 모든 주문이 정상 접수된 것으로 본다 */
class FakeExecutionRepository(private val rejections: Map<String, String> = emptyMap()) : ExecutionRepository {
    override suspend fun getExecutionList(startDate: String, endDate: String): List<ExecutionInfo> =
        rejections.map { (orderNo, reason) ->
            ExecutionInfo(ticker = "SOXL", orderNo = orderNo, filledPrice = 0.0, filledQuantity = 0.0, orderQuantity = 1.0, rejectReason = reason)
        }
}

fun checker(rejections: Map<String, String> = emptyMap()) = OrderRejectionChecker(FakeExecutionRepository(rejections), waitMillis = 0)
