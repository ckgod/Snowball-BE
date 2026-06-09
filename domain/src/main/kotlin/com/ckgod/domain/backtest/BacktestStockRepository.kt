package com.ckgod.domain.backtest

import com.ckgod.domain.model.*
import com.ckgod.domain.repository.StockRepository
import kotlinx.datetime.LocalDate
import java.util.concurrent.atomic.AtomicLong

class BacktestStockRepository(
    private val priceDataMap: Map<LocalDate, StockPriceHistory>
) : StockRepository {

    var currentDate: LocalDate = LocalDate(2024, 1, 1)
    private val orderSequence = AtomicLong(1)

    override suspend fun getCurrentPrice(stockCode: String, includeDayMarket: Boolean): MarketPrice? {
        val history = priceDataMap[currentDate] ?: return null
        return MarketPrice(
            ticker = stockCode,
            // previousClose는 주문 생성 기준가("직전 완료된 정규장 종가")로 쓰인다.
            // 백테스트에서 D일에 생성한 주문은 D+1일에 체결되므로 D일 종가가 그 기준가다.
            previousClose = history.close.toString(),
            price = history.close.toString(),
            changeRate = "0",
            open = history.open.toString(),
            high = history.high.toString(),
            low = history.low.toString(),
            volume = history.volume.toString(),
            krwPrice = "0",
            krwChangeAmount = "0",
            exchangeRate = "1450",
            currency = "USD"
        )
    }

    override suspend fun getExchangeRate(): Double = 1450.0

    override suspend fun postOrder(
        buyOrders: List<OrderRequest>,
        sellOrders: List<OrderRequest>
    ): List<OrderResponse> {
        val responses = mutableListOf<OrderResponse>()
        (sellOrders + buyOrders).forEach { order ->
            responses.add(
                OrderResponse(
                    request = order,
                    orderNo = "BT-${orderSequence.getAndIncrement()}",
                    orderTime = currentDate.toString()
                )
            )
        }
        return responses
    }
}
