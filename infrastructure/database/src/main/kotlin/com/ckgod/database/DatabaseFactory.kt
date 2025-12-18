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

                // division 컬럼이 새로 추가된 경우, 특정 종목의 division 값을 수정
                println("Setting correct division values for specific tickers...")
                exec("UPDATE investment_status SET division = 20 WHERE ticker = 'FNGU'")
                println("Division values updated successfully")
            } else {
                println("No database migration required")
            }
        }

        initialized = true
        println("Database initialized successfully")
    }
}