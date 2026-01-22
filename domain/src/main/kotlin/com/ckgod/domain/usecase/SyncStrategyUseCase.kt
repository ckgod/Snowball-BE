package com.ckgod.domain.usecase

import com.ckgod.domain.model.InvestmentStatus
import com.ckgod.domain.model.OrderSide
import com.ckgod.domain.model.OrderStatus
import com.ckgod.domain.repository.AccountRepository
import com.ckgod.domain.repository.ExecutionRepository
import com.ckgod.domain.repository.InvestmentStatusRepository
import com.ckgod.domain.repository.TradeHistoryRepository
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

/**
 * 종목 정산 UseCase
 */
class SyncStrategyUseCase(
    private val accountRepository: AccountRepository,
    private val investmentStatusRepository: InvestmentStatusRepository,
    private val executionRepository: ExecutionRepository,
    private val historyRepository: TradeHistoryRepository
) {
    private val logger = LoggerFactory.getLogger(SyncStrategyUseCase::class.java)

    suspend operator fun invoke(ticker: String? = null): List<SyncResult> {
        logger.info("[SyncStrategy] 시작 - ticker: ${ticker ?: "전체"}")
        syncOrderExecution()

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

    private suspend fun syncOrderExecution() {
        val executionList = executionRepository.getExecutionList()
        executionList.forEach { execution ->
            val status = when {
                execution.isFullyFilled -> OrderStatus.FILLED
                execution.isPartiallyFilled -> OrderStatus.PARTIAL
                else -> OrderStatus.CANCELED
            }
            val realizedAmount = historyRepository.findByOrderNo(execution.orderNo)?.let { history ->
                if (history.orderSide == OrderSide.SELL && status != OrderStatus.CANCELED) {
                    val originPrice = history.avgPrice * execution.filledQuantity
                    val totalPrice = execution.filledPrice * execution.filledQuantity
                    (totalPrice - originPrice)
                } else null
            }
            historyRepository.updateOrderStatus(
                execution.orderNo,
                status = status,
                filledQuantity = execution.filledQuantity.toInt(),
                filledPrice = execution.filledPrice,
                filledTime = LocalDateTime.now(),
                realizedProfitAmount = realizedAmount
            )
        }
    }

    private suspend fun syncSingle(currentStatus: InvestmentStatus): SyncResult? {
        val ticker = currentStatus.ticker

        // 잔고 조회
        val balance = accountRepository.getBalance(ticker)
        if (balance == null) {
            logger.warn("[SyncStrategy] [$ticker] 계좌에 보유 종목 없음")
        }

        val totalInvested = balance?.investedAmount?.toDoubleOrNull() ?: 0.0
        val avgPrice = balance?.avgPrice?.toDoubleOrNull() ?: 0.0
        val quantity = balance?.quantity?.toDoubleOrNull()?.toInt() ?: 0

//        val dailyProfit = try {
//            accountRepository.getDailyProfit(ticker).sum()
//        } catch (e: Exception) {
//            logger.error("[SyncStrategy] [$ticker] 일일 수익 조회 실패", e)
//            throw e
//        }

        val dailyProfit = historyRepository.findByYesterdayOrderTime(ticker).sumOf { history ->
            history.realizedProfitAmount
        }
        logger.info("[SyncStrategy] [$ticker] 일일 수익: $dailyProfit")

        // 상태 업데이트 및 저장
        val updatedStatus = currentStatus.updateFromAccount(
            name = balance?.name,
            totalInvested = totalInvested,
            avgPrice = avgPrice,
            quantity = quantity,
            dailyProfit = dailyProfit
        )

        try {
            investmentStatusRepository.save(updatedStatus)
            logger.info("[SyncStrategy] [$ticker] 정산 완료 - T값: ${"%.2f".format(updatedStatus.tValue)}, 별%: ${"%.2f".format(updatedStatus.starPercent)}%")
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
