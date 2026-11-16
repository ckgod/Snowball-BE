package com.ckgod.domain.usecase

import com.ckgod.domain.model.InvestmentStatus
import com.ckgod.domain.model.OrderSide
import com.ckgod.domain.model.OrderStatus
import com.ckgod.domain.repository.AccountRepository
import com.ckgod.domain.repository.ExecutionRepository
import com.ckgod.domain.repository.InvestmentStatusRepository
import com.ckgod.domain.repository.TradeHistoryRepository
import com.ckgod.domain.utils.systemKstClock
import com.ckgod.domain.utils.toKisDate
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 종목 정산 UseCase
 */
class SyncStrategyUseCase(
    private val accountRepository: AccountRepository,
    private val investmentStatusRepository: InvestmentStatusRepository,
    private val executionRepository: ExecutionRepository,
    private val historyRepository: TradeHistoryRepository,
    private val clock: Clock = systemKstClock
) {
    private val logger = LoggerFactory.getLogger(SyncStrategyUseCase::class.java)

    suspend operator fun invoke(ticker: String? = null): List<SyncResult> {
        logger.info("[SyncStrategy] 시작 - ticker: ${ticker ?: "전체"}")
        // 새벽 정산은 전날(KST) 18:05 에 낸 주문의 결과를 반영한다
        val orderDate = LocalDate.now(clock).minusDays(1)
        syncOrderExecution(orderDate)

        val targets = if (ticker != null) {
            val status = investmentStatusRepository.get(ticker)
            if (status != null) listOf(status) else emptyList()
        } else {
            investmentStatusRepository.findAll()
        }

        logger.info("[SyncStrategy] 대상 종목: ${targets.size}개")

        return targets.mapNotNull { status ->
            try {
                syncSingle(status, orderDate)
            } catch (e: Exception) {
                logger.error("[SyncStrategy] [${status.ticker}] 정산 실패", e)
                null // 실패한 종목은 제외
            }
        }
    }

    private suspend fun syncOrderExecution(orderDate: LocalDate) {
        val executionList = executionRepository.getExecutionList(orderDate.toKisDate(), orderDate.toKisDate())
        executionList.forEach { execution ->
            // 거래소가 거부한 주문을 미체결 취소(CANCELED)와 섞지 않는다
            execution.rejectReason?.let { reason ->
                historyRepository.markRejected(execution.orderNo, OrderRejectionChecker.describeRejection(reason))
                return@forEach
            }
            val status = when {
                execution.isFullyFilled -> OrderStatus.FILLED
                execution.isPartiallyFilled -> OrderStatus.PARTIAL
                else -> OrderStatus.CANCELED
            }
            val realizedAmount = historyRepository.findByOrderNo(execution.orderNo)?.let { history ->
                if (history.orderSide != OrderSide.SELL || status == OrderStatus.CANCELED) return@let null
                // 평단을 모르는 매도는 매도 대금 전체가 수익으로 잡혀 1회 매수금이 부풀므로 손익을 넣지 않는다
                if (history.avgPrice <= 0.0) {
                    logger.warn("[SyncStrategy] ${execution.orderNo} 매도 평단 없음 - 실현 손익 미반영")
                    return@let null
                }
                (execution.filledPrice - history.avgPrice) * execution.filledQuantity
            }
            historyRepository.updateOrderStatus(
                execution.orderNo,
                status = status,
                filledQuantity = execution.filledQuantity.toInt(),
                filledPrice = execution.filledPrice,
                filledTime = LocalDateTime.now(clock),
                realizedProfitAmount = realizedAmount
            )
        }
    }

    private suspend fun syncSingle(currentStatus: InvestmentStatus, orderDate: LocalDate): SyncResult? {
        val ticker = currentStatus.ticker

        val balance = accountRepository.getBalance(ticker)
        if (balance == null) {
            logger.warn("[SyncStrategy] [$ticker] 계좌에 보유 종목 없음")
        }

        val totalInvested = balance?.investedAmount?.toDoubleOrNull() ?: 0.0
        val avgPrice = balance?.avgPrice?.toDoubleOrNull() ?: 0.0
        val quantity = balance?.quantity?.toDoubleOrNull()?.toInt() ?: 0

        val dailyProfit = historyRepository.findByOrderDate(ticker, orderDate).sumOf { history ->
            history.realizedProfitAmount
        }
        logger.info("[SyncStrategy] [$ticker] 일일 수익: $dailyProfit")

        val updatedStatus = currentStatus.updateFromAccount(
            name = balance?.name,
            totalInvested = totalInvested,
            avgPrice = avgPrice,
            quantity = quantity,
            dailyProfit = dailyProfit,
            updatedAt = LocalDateTime.now(clock)
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
