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
            it[fullName] = status.fullName
            it[totalInvested] = status.totalInvested
            it[oneTimeAmount] = status.oneTimeAmount
            it[initialCapital] = status.initialCapital
            it[division] = status.division
            it[avgPrice] = status.avgPrice
            it[quantity] = status.quantity
            it[targetRate] = status.targetRate
            it[updatedAt] = status.updatedAt
            it[realizedTotalProfit] = status.realizedTotalProfit
        }
        status
    }

    private fun org.jetbrains.exposed.v1.core.ResultRow.toInvestmentStatus(): InvestmentStatus {
        return InvestmentStatus(
            ticker = this[InvestmentStatusTable.ticker],
            fullName = this[InvestmentStatusTable.fullName],
            totalInvested = this[InvestmentStatusTable.totalInvested],
            oneTimeAmount = this[InvestmentStatusTable.oneTimeAmount],
            initialCapital = this[InvestmentStatusTable.initialCapital],
            division = this[InvestmentStatusTable.division],
            avgPrice = this[InvestmentStatusTable.avgPrice],
            quantity = this[InvestmentStatusTable.quantity],
            targetRate = this[InvestmentStatusTable.targetRate],
            updatedAt = this[InvestmentStatusTable.updatedAt],
            realizedTotalProfit = this[InvestmentStatusTable.realizedTotalProfit],
        )
    }
}
