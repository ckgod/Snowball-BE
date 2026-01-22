package com.ckgod.domain.repository

import com.ckgod.domain.model.AccountStatus
import com.ckgod.domain.model.HoldingStock
import com.ckgod.domain.model.PresentAccountStatus
import com.ckgod.domain.model.TotalAsset

interface AccountRepository {
    suspend fun getAccountBalance(): AccountStatus

    suspend fun getPresentAccountBalance(): PresentAccountStatus

    /**
     * 특정 티커의 보유 정보 조회
     */
    suspend fun getBalance(ticker: String): HoldingStock? {
        val account = getAccountBalance()
        return account.holdings.find { it.ticker == ticker }
    }

    /**
     * 일일 수익 조회
     */
    suspend fun getDailyProfit(ticker: String): List<Double>

    suspend fun getTotalAsset(): TotalAsset
}