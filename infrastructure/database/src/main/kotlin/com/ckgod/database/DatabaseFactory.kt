package com.ckgod.database

import com.ckgod.database.auth.AuthTokens
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils
import java.io.File

object DatabaseFactory {
    private var initialized = false

    fun init() {
        if (initialized) {
            println("Database already initialized, skipping...")
            return
        }

        val dbFile = File("./database/snowball_db")
        val driverClassName = "org.h2.Driver"
        val jdbcUrl = "jdbc:h2:file:${dbFile.absolutePath};DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE"

        Database.connect(jdbcUrl, driverClassName)

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