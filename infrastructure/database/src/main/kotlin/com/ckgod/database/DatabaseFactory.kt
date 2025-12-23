package com.ckgod.database

import com.ckgod.database.auth.AuthTokens
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils

object DatabaseFactory {
    private var initialized = false

    fun init() {
        if (initialized) {
            println("Database already initialized, skipping...")
            return
        }

        val dbUrl = System.getenv("DB_URL") ?: throw IllegalStateException("DB_URL environment variable is required")
        val dbUser = System.getenv("DB_USER") ?: "root"
        val dbPassword = System.getenv("DB_PASSWORD") ?: ""

        println("Initializing MariaDB Connection...")
        Database.connect(
            url = dbUrl,
            driver = "org.mariadb.jdbc.Driver",
            user = dbUser,
            password = dbPassword
        )

        transaction {
            val allTables = arrayOf(
                AuthTokens,
                InvestmentStatusTable,
                TradeHistoryTable
            )
            val statements = MigrationUtils.statementsRequiredForDatabaseMigration(*allTables)
            if (statements.isNotEmpty()) {
                println("Database migration required. Executing ${statements.size} statement(s)...")
                statements.forEach { statement ->
                    println("  Executing: $statement")
                    exec(statement)
                }
                println("Database migration completed successfully")
            } else {
                println("No database migration required")
            }
        }

        initialized = true
        println("Database initialized successfully")
    }
}