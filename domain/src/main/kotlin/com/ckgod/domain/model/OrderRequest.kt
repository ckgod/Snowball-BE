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
    MOO("31"), // 장개시 시장가 (매도만)
    LOO("32"), // 장개시 지정가
    MOC("33"), // 장마감 시장가 (매도만)
    LOC("34"); // 장마감 지정가

    /** 가격 없이 나가는 시장가 계열 */
    val isMarket: Boolean get() = this == MOO || this == MOC

    /** KIS 미국 주문에서 이 방향으로 쓸 수 있는지 (매수는 LIMIT·LOO·LOC 만) */
    fun supports(side: OrderSide): Boolean = side == OrderSide.SELL || !isMarket
}

enum class Exchange(val code: String) {
    NASD("NASD"),
    AMEX("AMEX"),
    NYSE("NYSE");

    companion object {
        /** 운용 중인 종목은 알려진 거래소로, 그 밖은 나스닥으로 추정한다 */
        fun of(ticker: String): Exchange = when (ticker) {
            "TQQQ" -> NASD
            "SOXL", "FNGU", "SOXS" -> AMEX
            else -> NASD
        }

        fun fromCode(code: String): Exchange? = entries.find { it.code == code.uppercase() }
    }
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
