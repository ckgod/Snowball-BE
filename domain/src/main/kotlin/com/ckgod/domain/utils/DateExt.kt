package com.ckgod.domain.utils

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun yesterday(): String {
    val kstZone = ZoneId.of("Asia/Seoul")
    val yesterday = LocalDate.now(kstZone).minusDays(1)
    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    return yesterday.format(formatter)
}

fun beforeDay(daysToSubtract: Long = 1): String {
    val kstZone = ZoneId.of("Asia/Seoul")
    val yesterday = LocalDate.now(kstZone).minusDays(daysToSubtract)
    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    return yesterday.format(formatter)
}