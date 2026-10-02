package com.ckgod.domain.repository

import com.ckgod.domain.model.OpenOrder

/**
 * 이미 접수된 주문의 조회·정정·취소.
 */
interface OrderManagementRepository {
    suspend fun getOpenOrders(): List<OpenOrder>

    /** @return 정정 주문의 새 주문번호 */
    suspend fun modify(order: OpenOrder, quantity: Int, price: Double): String

    suspend fun cancel(order: OpenOrder)
}
