package com.ckgod.domain.model

data class OrderRequest(
    val ticker: String,
    val exchange: Exchange,
    val side: OrderSide,
    val type: OrderType,
    val price: Double,
    val quantity: Int,
    val crashRate: Double? = null
) {
    init {
        require(ticker.isNotBlank()) { "ticker must not be blank" }
        require(quantity > 0) { "quantity must be positive" }
    }
}

enum class OrderSide {
    BUY, SELL
}

enum class OrderType(val code: String) {
    LIMIT("00"), // 지정가
    MOC("33"), // 장마감 시장가 (매도에만 적용가능)
    LOC("34") // 장마감 지정가
}

enum class Exchange(val code: String) {
    NASD("NASD"),
    AMEX("AMEX"),
    NYSE("NYSE")
}

/** 주문 한 건을 보낸 결과. 접수(OrderResponse) 또는 실패(OrderRejection). */
sealed interface OrderOutcome {
    val request: OrderRequest
}

data class OrderResponse(
    override val request: OrderRequest,
    val orderNo: String,
    val orderTime: String
) : OrderOutcome

/**
 * 접수되지 못한 주문.
 *
 * @param reason 사용자에게 보여 줄 실패 사유. KIS 업무 거부면 `[msg_cd] msg1`,
 *   응답 자체를 못 받았으면 실제 접수 여부를 확인하라는 문구가 붙는다.
 */
data class OrderRejection(
    override val request: OrderRequest,
    val reason: String
) : OrderOutcome

/**
 * 주문 일괄 전송 결과. 실패 건을 버리지 않고 함께 돌려준다.
 */
data class OrderSubmission(
    val accepted: List<OrderResponse>,
    val rejected: List<OrderRejection>
)
