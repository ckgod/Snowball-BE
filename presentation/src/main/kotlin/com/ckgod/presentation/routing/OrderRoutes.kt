package com.ckgod.presentation.routing

import com.ckgod.domain.model.OpenOrder
import com.ckgod.domain.model.OrderActionResult
import com.ckgod.domain.model.OrderSide as DomainOrderSide
import com.ckgod.domain.model.OrderType as DomainOrderType
import com.ckgod.domain.usecase.ManageOrdersUseCase
import com.ckgod.domain.usecase.PlaceOrderUseCase
import com.ckgod.presentation.config.OrderGuard
import com.ckgod.snowball.model.ModifyOrderRequest
import com.ckgod.snowball.model.OpenOrderResponse
import com.ckgod.snowball.model.OpenOrdersResponse
import com.ckgod.snowball.model.OrderActionResponse
import com.ckgod.snowball.model.OrderSide
import com.ckgod.snowball.model.OrderType
import com.ckgod.snowball.model.PlaceOrderRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.json.Json

/**
 * GET /sb/orders/open — 미체결 주문 목록 (조회 키만 필요)
 */
suspend fun RoutingContext.openOrdersRoute(useCase: ManageOrdersUseCase) {
    try {
        val orders = useCase.getOpenOrders()
        call.respond(OpenOrdersResponse(orders = orders.map { it.toResponse() }))
    } catch (e: Exception) {
        e.printStackTrace()
        call.respond(HttpStatusCode.BadGateway, mapOf("error" to "미체결 조회 실패: ${e.message}"))
    }
}

private val orderJson = Json { ignoreUnknownKeys = true }

/**
 * POST /sb/orders — 수동 신규 주문 (주문 서명 필요)
 * Body: PlaceOrderRequest
 */
suspend fun RoutingContext.placeOrderRoute(useCase: PlaceOrderUseCase, guard: OrderGuard) {
    val body = call.receiveText()
    if (!guard.check(this, body)) return
    val request = runCatching { orderJson.decodeFromString<PlaceOrderRequest>(body) }.getOrElse {
        call.respond(HttpStatusCode.BadRequest, OrderActionResponse(false, "요청 형식이 잘못되었습니다"))
        return
    }
    val result = guard.idempotent(call.request.headers["Idempotency-Key"]) {
        useCase(
            PlaceOrderUseCase.Command(
                ticker = request.ticker,
                side = request.orderSide.toDomain(),
                type = request.orderType.toDomain(),
                price = request.price,
                quantity = request.quantity,
                exchangeCode = request.exchange
            )
        )
    }
    call.respond(result.toStatus(), result.toResponse())
}

/**
 * POST /sb/orders/{orderNo}/cancel — 남은 미체결 수량 전체 취소 (주문 서명 필요)
 */
suspend fun RoutingContext.cancelOrderRoute(useCase: ManageOrdersUseCase, guard: OrderGuard) {
    val body = call.receiveText()
    if (!guard.check(this, body)) return
    val orderNo = call.parameters["orderNo"].orEmpty()
    val result = guard.idempotent(call.request.headers["Idempotency-Key"]) { useCase.cancel(orderNo) }
    call.respond(result.toStatus(), result.toResponse())
}

/**
 * POST /sb/orders/{orderNo}/modify — 가격·수량 정정 (주문 서명 필요)
 * Body: ModifyOrderRequest
 */
suspend fun RoutingContext.modifyOrderRoute(useCase: ManageOrdersUseCase, guard: OrderGuard) {
    val body = call.receiveText()
    if (!guard.check(this, body)) return
    val orderNo = call.parameters["orderNo"].orEmpty()
    val request = runCatching { orderJson.decodeFromString<ModifyOrderRequest>(body) }.getOrElse {
        call.respond(HttpStatusCode.BadRequest, OrderActionResponse(false, "요청 형식이 잘못되었습니다"))
        return
    }
    val result = guard.idempotent(call.request.headers["Idempotency-Key"]) {
        useCase.modify(orderNo, request.price, request.quantity)
    }
    call.respond(result.toStatus(), result.toResponse())
}

private fun OrderSide.toDomain() = if (this == OrderSide.BUY) DomainOrderSide.BUY else DomainOrderSide.SELL

private fun OrderType.toDomain() = when (this) {
    OrderType.LIMIT -> DomainOrderType.LIMIT
    OrderType.LOC -> DomainOrderType.LOC
    OrderType.LOO -> DomainOrderType.LOO
    OrderType.MOC -> DomainOrderType.MOC
    OrderType.MOO -> DomainOrderType.MOO
}

private fun OrderActionResult.toStatus() =
    if (success) HttpStatusCode.OK else HttpStatusCode.UnprocessableEntity

private fun OrderActionResult.toResponse() =
    OrderActionResponse(success = success, message = message, newOrderNo = orderNo)

private fun OpenOrder.toResponse() = OpenOrderResponse(
    orderNo = orderNo,
    originalOrderNo = originalOrderNo,
    ticker = ticker,
    exchange = exchange,
    orderSide = if (side == DomainOrderSide.BUY) OrderSide.BUY else OrderSide.SELL,
    orderPrice = orderPrice,
    orderQuantity = orderQuantity,
    filledQuantity = filledQuantity,
    unfilledQuantity = unfilledQuantity,
    orderDate = orderDate,
    orderTime = orderTime
)
