package com.ckgod.kis.repository

import com.ckgod.domain.model.OpenOrder
import com.ckgod.domain.repository.OrderManagementRepository
import com.ckgod.kis.KisApiException
import com.ckgod.kis.api.KisApiService
import org.slf4j.LoggerFactory

class OrderManagementRepositoryImpl(
    private val kisApiService: KisApiService
) : OrderManagementRepository {

    private val logger = LoggerFactory.getLogger(OrderManagementRepositoryImpl::class.java)

    override suspend fun getOpenOrders(): List<OpenOrder> {
        val orders = mutableListOf<OpenOrder>()
        var trCont = ""
        var fKey = ""
        var nKey = ""
        var page = 0

        do {
            page++
            val response = kisApiService.getOpenOrders(trCont = trCont, fKey = fKey, nKey = nKey)
            val body = response.body
            if (body.returnCode != "0") {
                // 조회 실패를 빈 목록으로 숨기지 않는다 (미체결이 없는 것처럼 보이면 안 됨)
                throw KisApiException("미체결 조회 실패 [${body.messageCode}] ${body.message}")
            }
            orders += body.output.filter { it.orderNo.isNotBlank() }.map { it.toDomain() }

            val responseTrCont = response.headers["tr_cont"] ?: ""
            fKey = body.fKey.trim()
            nKey = body.nKey.trim()
            trCont = if (responseTrCont == "F" || responseTrCont == "M") "N" else ""
        } while (trCont == "N" && page < MAX_PAGES)

        logger.info("[OrderManagement] 미체결 조회 - ${orders.size}건 (페이지: $page)")
        return orders
    }

    override suspend fun modify(order: OpenOrder, quantity: Int, price: Double): String {
        val response = kisApiService.modifyOrder(order.exchange, order.ticker, order.orderNo, quantity, price)
        val newOrderNo = requireNotNull(response.output).orderNo
        logger.info("[OrderManagement] 정정 - ${order.ticker} ${order.orderNo} → $newOrderNo ${quantity}주 @$price")
        return newOrderNo
    }

    override suspend fun cancel(order: OpenOrder) {
        kisApiService.cancelOrder(order.exchange, order.ticker, order.orderNo, order.unfilledQuantity)
        logger.info("[OrderManagement] 취소 - ${order.ticker} ${order.orderNo} ${order.unfilledQuantity}주")
    }

    private companion object {
        const val MAX_PAGES = 10
    }
}
