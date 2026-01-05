package com.ckgod.kis.repository

import com.ckgod.domain.model.ExecutionInfo
import com.ckgod.domain.repository.ExecutionRepository
import com.ckgod.kis.api.KisApiService

class ExecutionRepositoryImpl(
    private val kisApiService: KisApiService
): ExecutionRepository {
    override suspend fun getExecutionList(): List<ExecutionInfo> {
        val ret = mutableListOf<ExecutionInfo>()
        /**
         * getExecution 호출 시 응답 헤더에 "tr_cont"값이 들어옴
         * tr_cont = "F" or "M" -> 다음 데이터 있음
         * tr_cont = "D" or "E" -> 마지막 데이터
         *
         * 다음 데이터 있을 경우에
         * 요청 헤더의 tr_cont값을 N으로 설정, 응답의 ctx_area_fk200, ctx_area_nk200 값을 요청의 쿼리 파라미터로 설정
         */
        val result = kisApiService.getExecution()


        val parse = result.output.map {
            it.toDomain()
        }
        println(parse.toString())
        return emptyList()
    }
}