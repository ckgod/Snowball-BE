package com.ckgod.domain.repository

import com.ckgod.domain.model.ExecutionInfo

interface ExecutionRepository {
    suspend fun getExecutionList() : List<ExecutionInfo>
}