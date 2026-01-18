package com.ckgod.kis.repository

import com.ckgod.domain.model.AccountStatus
import com.ckgod.domain.model.PresentAccountStatus
import com.ckgod.domain.repository.AccountRepository
import com.ckgod.domain.utils.yesterday
import com.ckgod.kis.api.KisApiService

class AccountRepositoryImpl(
    private val kisApiService: KisApiService,
): AccountRepository {
    override suspend fun getAccountBalance(): AccountStatus {
        val response = kisApiService.getAccountBalance()

        return response.toDomain()
    }

    override suspend fun getPresentAccountBalance() : PresentAccountStatus {
        val response = kisApiService.getPresentAccountBalance()
        return response.toDomain()
    }

    override suspend fun getDailyProfit(ticker: String): List<Double> {
        return kisApiService.getRecentDayProfit().details?.filter {
            it.ticker == ticker && it.tradeDay == yesterday()
        }?.map {
            it.realizedProfitAmount.toDouble()
        } ?: listOf()
    }
}