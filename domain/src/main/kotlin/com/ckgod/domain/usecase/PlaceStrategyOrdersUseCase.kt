package com.ckgod.domain.usecase

import com.ckgod.domain.model.InvestmentStatus
import com.ckgod.domain.model.OrderRequest
import com.ckgod.domain.model.TradeHistory
import com.ckgod.domain.repository.AccountRepository
import com.ckgod.domain.repository.InvestmentStatusRepository
import com.ckgod.domain.repository.StockRepository
import com.ckgod.domain.repository.TradeHistoryRepository
import com.ckgod.domain.strategy.OrderPlanner
import com.ckgod.domain.utils.systemKstClock
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.Clock

/**
 * 무한매수법 하루 주문을 계산해 실제로 전송하고 기록한다. 주문 Job 이 부른다.
 * 계산은 [OrderPlanner] 가 하고, 여기는 시세·잔고 조회, 전송, 기록, 거래소 거부 확인을 맡는다.
 */
class PlaceStrategyOrdersUseCase(
    private val stockRepository: StockRepository,
    private val accountRepository: AccountRepository,
    private val investmentStatusRepository: InvestmentStatusRepository,
    private val tradeHistoryRepository: TradeHistoryRepository,
    private val rejectionChecker: OrderRejectionChecker,
    private val clock: Clock = systemKstClock
) {
    private val logger = LoggerFactory.getLogger(PlaceStrategyOrdersUseCase::class.java)

    /** @param ticker null 이면 운용 중인 전체 종목 */
    suspend operator fun invoke(ticker: String? = null): List<OrderResult> {
        logger.info("[PlaceStrategyOrders] 시작 - ticker: ${ticker ?: "전체"}")

        val targets = if (ticker != null) {
            listOfNotNull(investmentStatusRepository.get(ticker))
        } else {
            investmentStatusRepository.findAll()
        }
        logger.info("[PlaceStrategyOrders] 대상 종목: ${targets.size}개")

        return targets.mapNotNull { status ->
            try {
                placeFor(status)
            } catch (e: Exception) {
                logger.error("[PlaceStrategyOrders] [${status.ticker}] 주문 실패", e)
                null
            }
        }
    }

    private suspend fun placeFor(status: InvestmentStatus): OrderResult? {
        val ticker = status.ticker

        // 기준가는 전일 정규장 종가다. 주문 시각(18:05 KST)엔 프리장이 열려 있어 현재가가 프리장 시세다.
        val marketPrice = stockRepository.getCurrentPrice(ticker)
        val basePrice = marketPrice?.previousClose?.toDoubleOrNull() ?: 0.0
        if (basePrice == 0.0) {
            logger.warn("[PlaceStrategyOrders] [$ticker] 기준가(전일 종가) 조회 실패")
            return null
        }

        val holding = accountRepository.getBalance(ticker)
        val currentQuantity = holding?.quantity?.toDoubleOrNull()?.toInt() ?: 0
        val currentAvgPrice = holding?.avgPrice?.toDoubleOrNull() ?: 0.0

        val plan = OrderPlanner.plan(status, basePrice, marketPrice?.price?.toDoubleOrNull(), currentQuantity)
        plan.notes.forEach { logger.info("[PlaceStrategyOrders] [$ticker] $it") }
        logger.info("[PlaceStrategyOrders] [$ticker] 주문 계산 - 매수: ${plan.buyOrders.size}개, 매도: ${plan.sellOrders.size}개")

        val submission = stockRepository.postOrder(plan.buyOrders, plan.sellOrders)
        logger.info("[PlaceStrategyOrders] [$ticker] 주문 전송 - 성공: ${submission.accepted.size}개, 실패: ${submission.rejected.size}개")

        (submission.accepted + submission.rejected).forEach { outcome ->
            tradeHistoryRepository.save(
                TradeHistory.of(
                    outcome = outcome,
                    tValue = status.tValue,
                    sellAvgPrice = currentAvgPrice,
                    orderTime = LocalDateTime.now(clock)
                )
            )
        }

        // 주문번호를 받은 뒤 거래소가 거부한 주문을 바로 REJECTED 로 고친다 (안 그러면 새벽 정산 전까지 PENDING 으로 보인다)
        rejectionChecker.findRejected(submission.accepted.map { it.orderNo }).forEach { (orderNo, reason) ->
            logger.warn("[PlaceStrategyOrders] [$ticker] 거래소 거부 - $orderNo: $reason")
            // 주문은 이미 나갔으니 기록 실패로 Job 을 실패시키지 않는다. 새벽 정산이 다시 고친다
            runCatching { tradeHistoryRepository.markRejected(orderNo, reason) }
                .onFailure { logger.error("[PlaceStrategyOrders] [$ticker] 거부 기록 실패 - $orderNo", it) }
        }

        return OrderResult(ticker, basePrice, plan.buyOrders, plan.sellOrders)
    }

    data class OrderResult(
        val ticker: String,
        val currentPrice: Double,
        val buyOrders: List<OrderRequest>,
        val sellOrders: List<OrderRequest>
    )
}
