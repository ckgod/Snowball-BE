package com.ckgod.database

import com.ckgod.domain.model.InvestmentStatus
import com.ckgod.domain.repository.InvestmentStatusRepository
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert

/**
 * InvestmentStatus Repository 구현체
 */
class InvestmentStatusRepositoryImpl : InvestmentStatusRepository {

    override suspend fun findAll(): List<InvestmentStatus> = transaction {
        InvestmentStatusTable.selectAll()
            .map { it.toInvestmentStatus() }
    }

    override suspend fun get(ticker: String): InvestmentStatus? = transaction {
        InvestmentStatusTable.selectAll()
            .where { InvestmentStatusTable.ticker eq ticker }
            .singleOrNull()
            ?.toInvestmentStatus()
    }

    override suspend fun save(status: InvestmentStatus): InvestmentStatus = transaction {
        InvestmentStatusTable.upsert {
            it[ticker] = status.ticker
            it[currentT] = status.currentT
            it[totalInvested] = status.totalInvested
            it[oneTimeAmount] = status.oneTimeAmount
            it[avgPrice] = status.avgPrice
            it[targetRate] = status.targetRate
            it[buyLocPrice] = status.buyLocPrice
            it[sellLocPrice] = status.sellLocPrice
            it[updatedAt] = status.updatedAt
        }
        status
    }

    private fun org.jetbrains.exposed.v1.core.ResultRow.toInvestmentStatus(): InvestmentStatus {
        return InvestmentStatus(
            ticker = this[InvestmentStatusTable.ticker],
            currentT = this[InvestmentStatusTable.currentT],
            totalInvested = this[InvestmentStatusTable.totalInvested],
            oneTimeAmount = this[InvestmentStatusTable.oneTimeAmount],
            avgPrice = this[InvestmentStatusTable.avgPrice],
            targetRate = this[InvestmentStatusTable.targetRate],
            buyLocPrice = this[InvestmentStatusTable.buyLocPrice],
            sellLocPrice = this[InvestmentStatusTable.sellLocPrice],
            updatedAt = this[InvestmentStatusTable.updatedAt]
        )
    }
}
