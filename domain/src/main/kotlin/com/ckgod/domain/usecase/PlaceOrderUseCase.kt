package com.ckgod.domain.usecase

import com.ckgod.domain.model.Exchange
import com.ckgod.domain.model.OrderActionResult
import com.ckgod.domain.model.OrderRequest
import com.ckgod.domain.model.OrderSide
import com.ckgod.domain.model.OrderStatus
import com.ckgod.domain.model.OrderType
import com.ckgod.domain.model.TradeHistory
import com.ckgod.domain.repository.AccountRepository
import com.ckgod.domain.repository.InvestmentStatusRepository
import com.ckgod.domain.repository.StockRepository
import com.ckgod.domain.repository.TradeHistoryRepository
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.ZoneId

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
    private val tradeHistoryRepository: TradeHistoryRepository
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
        record(request, accepted?.orderNo, rejected?.reason)

        return if (accepted != null) {
            logger.info("[PlaceOrder] $ticker ${command.side} ${command.type} ${command.quantity}주 @${request.price} → ${accepted.orderNo}")
            OrderActionResult(true, "주문이 접수됐습니다", accepted.orderNo)
        } else {
            OrderActionResult(false, rejected?.reason ?: "주문이 접수되지 않았습니다")
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
    private suspend fun record(request: OrderRequest, orderNo: String?, failReason: String?) {
        try {
            val status = investmentStatusRepository.get(request.ticker)
            // 매도 체결 시 실현 손익 계산에 쓰는 평단 (자동 주문과 같은 규칙)
            val avgPrice = if (request.side == OrderSide.SELL) {
                accountRepository.getBalance(request.ticker)?.avgPrice?.toDoubleOrNull() ?: 0.0
            } else 0.0

            tradeHistoryRepository.save(
                TradeHistory(
                    ticker = request.ticker,
                    orderNo = orderNo ?: "",
                    orderSide = request.side,
                    orderType = request.type,
                    orderPrice = request.price,
                    orderQuantity = request.quantity,
                    orderTime = LocalDateTime.now(ZoneId.of("Asia/Seoul")),
                    status = if (orderNo != null) OrderStatus.PENDING else OrderStatus.REJECTED,
                    tValue = status?.tValue ?: 0.0,
                    avgPrice = avgPrice,
                    failReason = failReason,
                    isManual = true
                )
            )
        } catch (e: Exception) {
            logger.error("[PlaceOrder] ${request.ticker} 주문 기록 실패 (KIS 처리 결과와 무관)", e)
        }
    }
}
