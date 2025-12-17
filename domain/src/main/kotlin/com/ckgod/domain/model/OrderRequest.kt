package com.ckgod.domain.model

data class OrderRequest(
    val ticker: String,
    val exchange: Exchange,
    val side: OrderSide,
    val type: OrderType,
    val price: Double,
    val quantity: Int
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
