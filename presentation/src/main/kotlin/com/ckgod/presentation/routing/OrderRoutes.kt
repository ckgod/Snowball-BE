package com.ckgod.presentation.routing

import com.ckgod.domain.model.OpenOrder
import com.ckgod.domain.model.OrderSide as DomainOrderSide
import com.ckgod.domain.usecase.ManageOrdersUseCase
import com.ckgod.presentation.config.OrderGuard
import com.ckgod.snowball.model.ModifyOrderRequest
import com.ckgod.snowball.model.OpenOrderResponse
import com.ckgod.snowball.model.OpenOrdersResponse
import com.ckgod.snowball.model.OrderActionResponse
import com.ckgod.snowball.model.OrderSide
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext

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

/**
 * POST /sb/orders/{orderNo}/cancel — 남은 미체결 수량 전체 취소 (X-Order-Key 필요)
 */
suspend fun RoutingContext.cancelOrderRoute(useCase: ManageOrdersUseCase, guard: OrderGuard) {
    if (!guard.check(this)) return
    val orderNo = call.parameters["orderNo"].orEmpty()
    val result = useCase.cancel(orderNo)
    call.respond(result.toStatus(), result.toResponse())
}

/**
 * POST /sb/orders/{orderNo}/modify — 가격·수량 정정 (X-Order-Key 필요)
 * Body: ModifyOrderRequest
 */
suspend fun RoutingContext.modifyOrderRoute(useCase: ManageOrdersUseCase, guard: OrderGuard) {
    if (!guard.check(this)) return
    val orderNo = call.parameters["orderNo"].orEmpty()
    val request = try {
        call.receive<ModifyOrderRequest>()
    } catch (e: Exception) {
        call.respond(HttpStatusCode.BadRequest, OrderActionResponse(false, "요청 형식이 잘못되었습니다"))
        return
    }
    val result = useCase.modify(orderNo, request.price, request.quantity)
    call.respond(result.toStatus(), result.toResponse())
}

private fun ManageOrdersUseCase.ActionResult.toStatus() =
    if (success) HttpStatusCode.OK else HttpStatusCode.UnprocessableEntity

private fun ManageOrdersUseCase.ActionResult.toResponse() =
    OrderActionResponse(success = success, message = message, newOrderNo = newOrderNo)

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
