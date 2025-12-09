package com.ckgod.data.auth

import java.time.LocalDateTime

data class AuthToken(
    val accessToken: String,
    val expireAt: LocalDateTime
)
