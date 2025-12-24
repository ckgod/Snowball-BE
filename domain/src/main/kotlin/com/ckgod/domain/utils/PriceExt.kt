package com.ckgod.domain.utils

import java.math.BigDecimal
import java.math.RoundingMode

fun Double.roundTo2Decimal(): Double {
    return BigDecimal.valueOf(this)
        .setScale(2, RoundingMode.HALF_UP).toDouble()
}