package com.ckgod.domain.usecase

import com.ckgod.domain.model.InvestmentStatus
import com.ckgod.domain.repository.AccountRepository
import com.ckgod.domain.repository.InvestmentStatusRepository
import org.slf4j.LoggerFactory

/**
 * 종목 정산 UseCase
 */
class SyncStrategyUseCase(
    private val accountRepository: AccountRepository,
    private val investmentStatusRepository: InvestmentStatusRepository
) {
    private val logger = LoggerFactory.getLogger(SyncStrategyUseCase::class.java)

    suspend operator fun invoke(ticker: String? = null): List<SyncResult> {
        logger.info("[SyncStrategy] 시작 - ticker: ${ticker ?: "전체"}")

        val targets = if (ticker != null) {
            val status = investmentStatusRepository.get(ticker)
            if (status != null) listOf(status) else emptyList()
        } else {
            investmentStatusRepository.findAll()
        }

        logger.info("[SyncStrategy] 대상 종목: ${targets.size}개")

        return targets.mapNotNull { status ->
            try {
                syncSingle(status)
            } catch (e: Exception) {
                logger.error("[SyncStrategy] [${status.ticker}] 정산 실패", e)
                null // 실패한 종목은 제외
            }
        }
    }

    private suspend fun syncSingle(currentStatus: InvestmentStatus): SyncResult? {
        val ticker = currentStatus.ticker
        logger.debug("[SyncStrategy] [$ticker] 단일 종목 정산 시작")

        // 1. 한투 API - 잔고 조회
        logger.debug("[SyncStrategy] [$ticker] Step 1: 잔고 조회")
        val balance = accountRepository.getBalance(ticker)
        if (balance == null) {
            logger.warn("[SyncStrategy] [$ticker] 잔고 조회 실패 - 계좌에 보유 종목 없음")
            return null
        }

        val totalInvested = balance.investedAmount.toDoubleOrNull() ?: 0.0
        val avgPrice = balance.avgPrice.toDoubleOrNull() ?: 0.0
        logger.debug("[SyncStrategy] [$ticker] 잔고 정보 - 누적투자: $totalInvested, 평단가: $avgPrice")

        // 2. 한투 API - 기간손익 조회
        logger.debug("[SyncStrategy] [$ticker] Step 2: 일일 수익 조회")
        val dailyProfit = try {
            accountRepository.getDailyProfit(ticker)
        } catch (e: Exception) {
            logger.error("[SyncStrategy] [$ticker] 일일 수익 조회 실패", e)
            throw e
        }
        logger.debug("[SyncStrategy] [$ticker] 일일 수익: $dailyProfit")

        // 3. 상태 업데이트
        logger.debug("[SyncStrategy] [$ticker] Step 3: 상태 업데이트 계산")
        val updatedStatus = currentStatus.updateFromAccount(
            totalInvested = totalInvested,
            avgPrice = avgPrice,
            dailyProfit = dailyProfit
        )
        logger.debug("[SyncStrategy] [$ticker] 업데이트 - T값: ${currentStatus.tValue} → ${updatedStatus.tValue}")

        // 4. DB 저장
        logger.debug("[SyncStrategy] [$ticker] Step 4: DB 저장")
        try {
            investmentStatusRepository.save(updatedStatus)
            logger.info("[SyncStrategy] [$ticker] 정산 완료 - T값: ${updatedStatus.tValue}, 별%: ${updatedStatus.starPercent}%")
        } catch (e: Exception) {
            logger.error("[SyncStrategy] [$ticker] DB 저장 실패", e)
            throw e
        }

        return SyncResult(
            ticker = ticker,
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
