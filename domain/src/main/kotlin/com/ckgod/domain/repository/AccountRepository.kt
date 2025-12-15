package com.ckgod.domain.repository

import com.ckgod.domain.model.AccountStatus
import com.ckgod.domain.model.StockHolding

interface AccountRepository {
    suspend fun getAccountBalance(): AccountStatus

    /**
     * 특정 티커의 보유 정보 조회
     */
    suspend fun getBalance(ticker: String): StockHolding? {
        val account = getAccountBalance()
        return account.holdings.find { it.ticker == ticker }
    }

    /**
     * 일일 수익 조회 (간단 구현 - 현재는 0 리턴)
     */
    suspend fun getDailyProfit(ticker: String): Double {
        // TODO: 실제 구현 필요
        return 0.0
    }
}