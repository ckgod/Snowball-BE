package com.ckgod.kis.websokets

import com.ckgod.domain.model.OrderStatus
import com.ckgod.domain.repository.TradeHistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.time.LocalDateTime


class ExecutionNotificationProcessor(
    private val tradeHistoryRepository: TradeHistoryRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val executionChannel = Channel<RealtimeExecutionNotification>(capacity = 1000)

    fun start() {
        scope.launch {
            processExecutionNotifications()
        }
    }

    fun stop() {
        executionChannel.close()
        scope.cancel()
    }

    suspend fun process(notification: RealtimeExecutionNotification) {
        logger.info("process notification: $notification")
        // 주문 접소 통보는 필터링 (orderQuantity가 빈 값)
        val hasOrderQuantity = notification.orderQuantity.trim().isNotEmpty()

        if (hasOrderQuantity) {
            executionChannel.send(notification)
        }
    }

    private suspend fun processExecutionNotifications() {
        executionChannel.receiveAsFlow().collect { notification ->
            try {
                processNotification(notification)
            } catch (e: Exception) {
                logger.error("체결 통보 처리 실패 [주문번호: ${notification.orderNumber}]: ${e.message}", e)
            }
        }
    }

    private suspend fun processNotification(notification: RealtimeExecutionNotification) {
        when(notification.executionFlag) {
            "1" -> { // 미체결
                tradeHistoryRepository.updateOrderStatus(
                    orderNo = notification.orderNumber,
                    status = OrderStatus.CANCELED,
                    filledQuantity = 0,
                    filledPrice = 0.0,
                    filledTime = LocalDateTime.now()
                )
                logger.info("[${notification.stockShortCode}] 미체결 : ${notification.orderNumber}")
            }
            "2" -> { // 체결
                val filledQty = notification.executionQuantity.trim().toIntOrNull() ?: 0
                val orderQty = notification.orderQuantity.trim().toIntOrNull() ?: 0
                val filledPrice = parsePrice(notification.executionPrice)

                val status = if (filledQty >= orderQty) {
                    OrderStatus.FILLED
                } else {
                    OrderStatus.PARTIAL
                }

                tradeHistoryRepository.updateOrderStatus(
                    orderNo = notification.orderNumber,
                    status = status,
                    filledQuantity = filledQty,
                    filledPrice = filledPrice,
                    filledTime = LocalDateTime.now()
                )
                logger.info("[${notification.stockShortCode}] 체결: ${notification.orderNumber} $status ${filledQty}/${orderQty} @$${filledPrice}")
            }
        }
    }

    private fun parsePrice(priceStr: String): Double {
        if (priceStr.isEmpty()) return 0.0
        val priceInt = priceStr.trim().toLongOrNull() ?: return 0.0
        return priceInt / 10000.0
    }
}
