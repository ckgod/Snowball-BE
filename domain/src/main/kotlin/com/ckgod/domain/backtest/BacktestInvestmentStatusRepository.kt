package com.ckgod.domain.backtest

import com.ckgod.domain.model.InvestmentStatus
import com.ckgod.domain.repository.InvestmentStatusRepository

class BacktestInvestmentStatusRepository : InvestmentStatusRepository {

    private val statuses = mutableMapOf<String, InvestmentStatus>()

    override suspend fun findAll(): List<InvestmentStatus> = statuses.values.toList()

    override suspend fun get(ticker: String): InvestmentStatus? = statuses[ticker]

    override suspend fun save(status: InvestmentStatus): InvestmentStatus {
        statuses[status.ticker] = status
        return status
    }

    fun initialize(status: InvestmentStatus) {
        statuses[status.ticker] = status
    }
}
