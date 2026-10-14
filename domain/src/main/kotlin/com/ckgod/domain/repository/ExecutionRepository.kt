package com.ckgod.domain.repository

import com.ckgod.domain.model.ExecutionInfo
import com.ckgod.domain.utils.yesterday

interface ExecutionRepository {
    /**
     * 주문일(KST, yyyyMMdd) 범위의 주문·체결 내역. 기본값은 어제 하루 (새벽 정산용).
     */
    suspend fun getExecutionList(startDate: String = yesterday(), endDate: String = yesterday()): List<ExecutionInfo>
}
