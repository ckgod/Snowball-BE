package com.ckgod.kis.repository

import com.ckgod.domain.model.ExecutionInfo
import com.ckgod.domain.repository.ExecutionRepository
import com.ckgod.kis.api.KisApiService
import org.slf4j.LoggerFactory

class ExecutionRepositoryImpl(
    private val kisApiService: KisApiService
): ExecutionRepository {
    private val logger = LoggerFactory.getLogger(ExecutionRepositoryImpl::class.java)

    override suspend fun getExecutionList(): List<ExecutionInfo> {
        val allExecutions = mutableListOf<ExecutionInfo>()

        var trCont = ""
        var fKey = ""
        var nKey = ""
        var pageCount = 0

        do {
            pageCount++
            logger.info("[ExecutionRepository] 체결 내역 조회 중... (페이지: $pageCount, tr_cont 요청값: '$trCont')")

            val response = kisApiService.getExecution(
                trCont = trCont,
                fKey = fKey,
                nKey = nKey
            )

            val executions = response.body.output.map { it.toDomain() }
            allExecutions.addAll(executions)

            logger.info("[ExecutionRepository] 페이지 $pageCount 조회 완료 - ${executions.size}건")

            val responseTrCont = response.headers["tr_cont"] ?: ""
            fKey = response.body.fKey.trim()
            nKey = response.body.nKey.trim()

            logger.info("[ExecutionRepository] tr_cont 응답값: '$responseTrCont', fKey: $fKey, nKey: $nKey")

            trCont = if (responseTrCont == "F" || responseTrCont == "M") {
                "N"
            } else {
                ""
            }

        } while (trCont == "N")

        logger.info("[ExecutionRepository] 전체 체결 내역 조회 완료 - 총 ${allExecutions.size}건 (페이지: $pageCount)")

        return allExecutions
    }
}