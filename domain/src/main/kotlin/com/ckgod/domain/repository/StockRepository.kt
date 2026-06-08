package com.ckgod.domain.repository

import com.ckgod.domain.model.MarketPrice
import com.ckgod.domain.model.OrderRequest
import com.ckgod.domain.model.OrderResponse

interface StockRepository {
    /**
     * @param includeDayMarket true면 주간거래(데이마켓) 세션 중 실시간 시세를 우선 조회한다.
     *   대시보드 조회용. 주문 생성·환율 조회는 정규장 기준이어야 하므로 기본값(false)을 사용한다.
     */
    suspend fun getCurrentPrice(stockCode: String, includeDayMarket: Boolean = false): MarketPrice?

    suspend fun getExchangeRate(): Double

    suspend fun postOrder(
        buyOrders: List<OrderRequest> = emptyList(),
        sellOrders: List<OrderRequest> = emptyList()
    ): List<OrderResponse>
}
