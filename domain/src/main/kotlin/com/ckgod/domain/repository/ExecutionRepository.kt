package com.ckgod.domain.repository

import com.ckgod.domain.model.ExecutionInfo

interface ExecutionRepository {
    /** 주문일(KST, yyyyMMdd) 범위의 주문·체결 내역 */
    suspend fun getExecutionList(startDate: String, endDate: String): List<ExecutionInfo>
}
