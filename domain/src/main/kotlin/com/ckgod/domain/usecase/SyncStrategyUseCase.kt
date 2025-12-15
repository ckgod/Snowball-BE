package com.ckgod.domain.usecase

import com.ckgod.domain.model.InvestmentStatus
import com.ckgod.domain.repository.AccountRepository
import com.ckgod.domain.repository.InvestmentStatusRepository

/**
 * 종목 정산 UseCase
 */
class SyncStrategyUseCase(
    private val accountRepository: AccountRepository,
    private val investmentStatusRepository: InvestmentStatusRepository
) {
    suspend operator fun invoke(ticker: String? = null): List<SyncResult> {
        val targets = if (ticker != null) {
            val status = investmentStatusRepository.get(ticker)
            if (status != null) listOf(status) else emptyList()
        } else {
            investmentStatusRepository.findAll()
        }

        return targets.mapNotNull { status ->
            try {
                syncSingle(status)
            } catch (e: Exception) {
                null // 실패한 종목은 제외
            }
        }
    }

    private suspend fun syncSingle(currentStatus: InvestmentStatus): SyncResult? {
        // 한투 API - 잔고 조회
        val balance = accountRepository.getBalance(currentStatus.ticker) ?: return null

        val totalInvested = balance.investedAmount.toDoubleOrNull() ?: 0.0
        val avgPrice = balance.avgPrice.toDoubleOrNull() ?: 0.0

        // 한투 API - 기간손익 조회
        val dailyProfit = accountRepository.getDailyProfit(currentStatus.ticker)

        // 상태 업데이트
        val updatedStatus = currentStatus.updateFromAccount(
            totalInvested = totalInvested,
            avgPrice = avgPrice,
            dailyProfit = dailyProfit
        )

        // DB 저장
        investmentStatusRepository.save(updatedStatus)

        return SyncResult(
            ticker = currentStatus.ticker,
            before = currentStatus,
            after = updatedStatus,
            dailyProfit = dailyProfit
        )
    }

    data class SyncResult(
        val ticker: String,
        val before: InvestmentStatus,
        val after: InvestmentStatus,
        val dailyProfit: Double
    )
}
