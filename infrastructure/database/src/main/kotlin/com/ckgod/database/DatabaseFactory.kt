package com.ckgod.database

import com.ckgod.database.auth.AuthTokens
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils
import org.slf4j.LoggerFactory

object DatabaseFactory {
    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)
    private var initialized = false

    fun init() {
        if (initialized) {
            logger.info("Database already initialized, skipping...")
            return
        }

        val dbUrl = System.getenv("DB_URL") ?: throw IllegalStateException("DB_URL environment variable is required")
        val dbUser = System.getenv("DB_USER") ?: "root"
        val dbPassword = System.getenv("DB_PASSWORD") ?: ""

        logger.info("Initializing MariaDB Connection...")
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
                TradeHistoryTable,
                StockPriceHistoryTable
            )
            val statements = MigrationUtils.statementsRequiredForDatabaseMigration(*allTables)
            if (statements.isNotEmpty()) {
                logger.warn("Database migration required. Executing ${statements.size} statement(s)...")
                statements.forEach { statement ->
                    logger.warn("  Executing: $statement")
                    exec(statement)
                }
                logger.info("Database migration completed successfully")
            } else {
                logger.info("No database migration required")
            }
        }

        initialized = true
        logger.info("Database initialized successfully")
    }
}