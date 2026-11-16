package com.ckgod.domain.utils

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 운용 기준 시간대. 주문·정산 Job 과 KIS 조회 날짜가 모두 한국 날짜 기준이다 */
val KST: ZoneId = ZoneId.of("Asia/Seoul")

/** 실제 시각. 테스트는 Clock.fixed 를 넘긴다 */
val systemKstClock: Clock = Clock.system(KST)

private val KIS_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

/** KIS 조회 파라미터 형식 (yyyyMMdd) */
fun LocalDate.toKisDate(): String = format(KIS_DATE)
