package com.ckgod.kis.response

/**
 * KIS 응답의 숫자 문자열을 읽는다. 빈 값은 0 이고, 숫자가 아니면 예외다.
 * 못 읽은 값을 0 으로 바꾸면 평단·수량 0 이 정산에 그대로 들어간다.
 */
internal fun String.kisNumber(): Double = if (isBlank()) 0.0 else trim().toDouble()
