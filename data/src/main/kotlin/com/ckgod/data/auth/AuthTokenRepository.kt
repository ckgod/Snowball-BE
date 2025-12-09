package com.ckgod.data.auth

import com.ckgod.database.auth.AuthTokens
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import java.time.LocalDateTime

class AuthTokenRepository {

    fun getToken(key: String): AuthToken? = transaction {
        AuthTokens.selectAll()
            .where { AuthTokens.key eq key }
            .firstOrNull()
            ?.let {
                AuthToken(
                    accessToken = it[AuthTokens.accessToken],
                    expireAt = it[AuthTokens.expireAt]
                )
            }
    }

    fun saveToken(key: String, accessToken: String, expireAt: LocalDateTime): InsertStatement<Number> = transaction {
        AuthTokens.deleteWhere { AuthTokens.key eq key }

        AuthTokens.insert {
            it[AuthTokens.key] = key
            it[AuthTokens.accessToken] = accessToken
            it[AuthTokens.expireAt] = expireAt
        }
    }

    fun deleteToken(key: String): Int = transaction {
        AuthTokens.deleteWhere { AuthTokens.key eq key }
    }

    fun deleteAllTokens(): Int = transaction {
        AuthTokens.deleteAll()
    }
}
