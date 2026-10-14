package com.ckgod.domain.usecase

import com.ckgod.domain.model.OpenOrder
import com.ckgod.domain.model.OrderActionResult
import com.ckgod.domain.model.OrderStatus
import com.ckgod.domain.repository.OrderManagementRepository
import com.ckgod.domain.repository.TradeHistoryRepository
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 미체결 주문 조회·정정·취소.
 *
 * 대상 주문은 항상 KIS 미체결 목록에서 다시 찾는다. 앱이 보낸 값(종목·거래소·수량)을 믿지 않고
 * 실제로 남아 있는 주문만 건드리기 위해서다.
 */
class ManageOrdersUseCase(
    private val orderManagementRepository: OrderManagementRepository,
    private val tradeHistoryRepository: TradeHistoryRepository,
    private val rejectionChecker: OrderRejectionChecker
) {
    private val logger = LoggerFactory.getLogger(ManageOrdersUseCase::class.java)

    suspend fun getOpenOrders(): List<OpenOrder> = orderManagementRepository.getOpenOrders()

    suspend fun cancel(orderNo: String): OrderActionResult {
        val order = findOpenOrder(orderNo)
            ?: return OrderActionResult(false, "미체결 주문이 아닙니다 (이미 체결·취소됐을 수 있음)")

        return runAction("취소", order) {
            orderManagementRepository.cancel(order)
            recordQuietly(order) { tradeHistoryRepository.updateStatus(order.orderNo, OrderStatus.CANCELED) }
            OrderActionResult(true, "취소 요청이 접수됐습니다")
        }
    }

    suspend fun modify(orderNo: String, price: Double, quantity: Int?): OrderActionResult {
        val order = findOpenOrder(orderNo)
            ?: return OrderActionResult(false, "미체결 주문이 아닙니다 (이미 체결·취소됐을 수 있음)")

        val newQuantity = quantity ?: order.unfilledQuantity
        if (price <= 0.0) return OrderActionResult(false, "가격은 0보다 커야 합니다")
        if (newQuantity <= 0 || newQuantity > order.unfilledQuantity) {
            return OrderActionResult(false, "수량은 1 ~ ${order.unfilledQuantity}(미체결 수량) 사이여야 합니다")
        }

        return runAction("정정", order) {
            val newOrderNo = orderManagementRepository.modify(order, newQuantity, price)

            // 새 주문번호를 받아도 거래소가 정정을 거부할 수 있다. 그때 원주문은 그대로 살아 있다.
            val rejectReason = rejectionChecker.findRejected(listOf(newOrderNo))[newOrderNo]

            recordQuietly(order) {
                val origin = tradeHistoryRepository.findByOrderNo(order.orderNo) ?: return@recordQuietly
                // 정정되면 원주문은 사라지고 새 주문번호로 이어진다. 거부면 원주문 기록은 건드리지 않는다.
                if (rejectReason == null) tradeHistoryRepository.updateStatus(origin.orderNo, OrderStatus.CANCELED)
                val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                tradeHistoryRepository.save(
                    origin.copy(
                        id = 0,
                        orderNo = newOrderNo,
                        orderPrice = price,
                        orderQuantity = newQuantity,
                        orderTime = now,
                        status = if (rejectReason == null) OrderStatus.PENDING else OrderStatus.REJECTED,
                        filledQuantity = 0,
                        filledPrice = 0.0,
                        filledTime = null,
                        realizedProfitAmount = 0.0,
                        failReason = rejectReason,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }

            if (rejectReason != null) {
                logger.warn("[ManageOrders] ${order.ticker} ${order.orderNo} 정정 거부 - $newOrderNo: $rejectReason")
                OrderActionResult(false, "정정이 거부됐습니다. 원주문은 그대로 남아 있습니다 - $rejectReason", newOrderNo)
            } else {
                OrderActionResult(true, "정정 요청이 접수됐습니다", newOrderNo)
            }
        }
    }

    private suspend fun findOpenOrder(orderNo: String): OpenOrder? {
        if (orderNo.isBlank()) return null
        return orderManagementRepository.getOpenOrders().find { it.orderNo == orderNo }
    }

    /**
     * KIS 요청이 이미 접수된 뒤의 DB 기록 실패는 결과를 실패로 바꾸지 않는다.
     * (실제 주문은 처리됐는데 앱에 실패로 보이면 같은 요청을 다시 보내게 된다)
     */
    private suspend fun recordQuietly(order: OpenOrder, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            logger.error("[ManageOrders] ${order.ticker} ${order.orderNo} 기록 갱신 실패 (KIS 처리는 완료)", e)
        }
    }

    private suspend fun runAction(name: String, order: OpenOrder, block: suspend () -> OrderActionResult): OrderActionResult {
        return try {
            block()
        } catch (e: Exception) {
            logger.error("[ManageOrders] ${order.ticker} ${order.orderNo} $name 실패", e)
            OrderActionResult(false, "$name 실패: ${e.message ?: e::class.simpleName}")
        }
    }
}
