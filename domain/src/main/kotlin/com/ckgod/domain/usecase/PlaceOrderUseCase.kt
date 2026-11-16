package com.ckgod.domain.usecase

import com.ckgod.domain.model.Exchange
import com.ckgod.domain.model.InvestmentStatus
import com.ckgod.domain.model.OrderActionResult
import com.ckgod.domain.model.OrderOutcome
import com.ckgod.domain.model.OrderRejection
import com.ckgod.domain.model.OrderRequest
import com.ckgod.domain.model.OrderSide
import com.ckgod.domain.model.OrderType
import com.ckgod.domain.model.TradeHistory
import com.ckgod.domain.repository.AccountRepository
import com.ckgod.domain.repository.InvestmentStatusRepository
import com.ckgod.domain.repository.StockRepository
import com.ckgod.domain.repository.TradeHistoryRepository
import kotlinx.coroutines.CancellationException
import com.ckgod.domain.utils.systemKstClock
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.Clock

/**
 * 앱에서 직접 넣는 신규 주문.
 *
 * 자동매매 주문과 같은 전송·기록 경로를 쓰되 isManual 로 구분한다.
 * 금액 상한·가격 범위 제한은 두지 않는다 (사용자 결정, 2026-10-06). KIS 가 받지 않는 조합만 미리 막는다.
 */
class PlaceOrderUseCase(
    private val stockRepository: StockRepository,
    private val accountRepository: AccountRepository,
    private val investmentStatusRepository: InvestmentStatusRepository,
    private val tradeHistoryRepository: TradeHistoryRepository,
    private val rejectionChecker: OrderRejectionChecker,
    private val clock: Clock = systemKstClock
) {
    private val logger = LoggerFactory.getLogger(PlaceOrderUseCase::class.java)

    data class Command(
        val ticker: String,
        val side: OrderSide,
        val type: OrderType,
        val price: Double,
        val quantity: Int,
        val exchangeCode: String? = null
    )

    suspend operator fun invoke(command: Command): OrderActionResult {
        val ticker = command.ticker.trim().uppercase()
        validate(command, ticker)?.let { return OrderActionResult(false, it) }

        val exchange = command.exchangeCode?.let { Exchange.fromCode(it) } ?: Exchange.of(ticker)
        val request = OrderRequest(
            ticker = ticker,
            exchange = exchange,
            side = command.side,
            type = command.type,
            price = if (command.type.isMarket) 0.0 else command.price,
            quantity = command.quantity
        )

        val submission = try {
            if (command.side == OrderSide.BUY) {
                stockRepository.postOrder(buyOrders = listOf(request))
            } else {
                stockRepository.postOrder(sellOrders = listOf(request))
            }
        } catch (e: Exception) {
            logger.error("[PlaceOrder] $ticker 주문 전송 실패", e)
            return OrderActionResult(false, "주문 전송 실패: ${e.message ?: e::class.simpleName}")
        }

        val accepted = submission.accepted.firstOrNull()
        val rejected = submission.rejected.firstOrNull()
        // 주문번호를 받은 뒤 거래소가 거부할 수 있다 (예: 지정가가 호가에서 30% 넘게 벗어남)
        val exchangeRejectReason = accepted?.let { rejectionChecker.findRejected(listOf(it.orderNo))[it.orderNo] }
        val outcome = accepted ?: rejected ?: OrderRejection(request, NOT_ACCEPTED)
        record(outcome, exchangeRejectReason)

        return when {
            accepted == null -> OrderActionResult(false, rejected?.reason ?: NOT_ACCEPTED)
            exchangeRejectReason != null -> {
                logger.warn("[PlaceOrder] $ticker ${accepted.orderNo} 거래소 거부: $exchangeRejectReason")
                OrderActionResult(false, "주문이 거부됐습니다 - $exchangeRejectReason", accepted.orderNo)
            }
            else -> {
                logger.info("[PlaceOrder] $ticker ${command.side} ${command.type} ${command.quantity}주 @${request.price} → ${accepted.orderNo}")
                OrderActionResult(true, "주문이 접수됐습니다", accepted.orderNo)
            }
        }
    }

    private fun validate(command: Command, ticker: String): String? = when {
        ticker.isBlank() -> "종목을 입력해 주세요"
        command.quantity <= 0 -> "수량은 1주 이상이어야 합니다"
        !command.type.supports(command.side) -> "${command.type} 는 매도에서만 쓸 수 있습니다"
        !command.type.isMarket && command.price <= 0.0 -> "가격은 0보다 커야 합니다"
        command.exchangeCode != null && Exchange.fromCode(command.exchangeCode) == null ->
            "거래소 코드가 올바르지 않습니다 (NASD, NYSE, AMEX)"
        else -> null
    }

    /**
     * KIS 처리 결과와 별개로, 기록 실패가 응답을 실패로 바꾸지 않게 한다 (같은 주문을 다시 넣게 되므로).
     */
    private suspend fun record(outcome: OrderOutcome, exchangeRejectReason: String?) {
        val ticker = outcome.request.ticker
        try {
            val status = investmentStatusRepository.get(ticker)
            tradeHistoryRepository.save(
                TradeHistory.of(
                    outcome = outcome,
                    tValue = status?.tValue ?: 0.0,
                    sellAvgPrice = if (outcome.request.side == OrderSide.SELL) sellAvgPrice(ticker, status) else 0.0,
                    orderTime = LocalDateTime.now(clock),
                    isManual = true,
                    exchangeRejectReason = exchangeRejectReason
                )
            )
        } catch (e: Exception) {
            logger.error("[PlaceOrder] $ticker 주문 기록 실패 (KIS 처리 결과와 무관)", e)
        }
    }

    /**
     * 매도 체결 시 실현 손익 계산에 쓰는 평단. 잔고 조회가 실패해도 주문 기록은 남겨야 하므로
     * 마지막 정산 평단으로 대신한다.
     */
    private suspend fun sellAvgPrice(ticker: String, status: InvestmentStatus?): Double {
        val accountAvg = try {
            accountRepository.getBalance(ticker)?.avgPrice?.toDoubleOrNull()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn("[PlaceOrder] $ticker 잔고 조회 실패 - 정산 평단으로 기록: ${e.message}")
            null
        }
        return accountAvg ?: status?.avgPrice ?: 0.0
    }

    private companion object {
        const val NOT_ACCEPTED = "주문이 접수되지 않았습니다"
    }
}
