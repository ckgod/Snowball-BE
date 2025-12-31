package com.ckgod.presentation.mapper

import com.ckgod.domain.model.OrderStatus
import com.ckgod.domain.model.OrderType as DomainOrderType
import com.ckgod.domain.model.OrderSide as DomainOrderSide
import com.ckgod.domain.model.TradeHistory
import com.ckgod.snowball.model.OrderSide
import com.ckgod.snowball.model.OrderType
import com.ckgod.snowball.model.TradeHistoryResponse
import com.ckgod.snowball.model.TradeStatus

object TradeHistoryMapper {
    fun toResponse(history: TradeHistory): TradeHistoryResponse {
        val orderSide = when(history.orderSide) {
            DomainOrderSide.BUY -> OrderSide.BUY
            DomainOrderSide.SELL -> OrderSide.SELL
        }
        val orderType = when(history.orderType) {
            DomainOrderType.LIMIT -> OrderType.LIMIT
            DomainOrderType.MOC -> OrderType.MOC
            DomainOrderType.LOC -> OrderType.LOC
        }
        val orderStatus = when(history.status) {
            OrderStatus.PENDING -> TradeStatus.PENDING
            OrderStatus.FILLED -> TradeStatus.FILLED
            OrderStatus.CANCELED -> TradeStatus.CANCELED
            OrderStatus.PARTIAL -> TradeStatus.PARTIAL
        }

        return TradeHistoryResponse(
            id = history.id,
            ticker = history.ticker,
            orderNo = history.orderNo,
            orderSide = orderSide,
            orderType = orderType,
            orderPrice = history.orderPrice,
            orderQuantity = history.orderQuantity,
            orderTime = history.orderTime.toString(),
            tradeStatus = orderStatus,
            filledQuantity = history.filledQuantity,
            filledPrice = history.filledPrice,
            filledTime = history.filledTime?.toString(),
            tValue = history.tValue,
            createdAt = history.createdAt.toString(),
            crashRate = history.crashRate
        )
    }
}