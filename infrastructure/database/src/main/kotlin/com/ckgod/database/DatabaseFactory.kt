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
            // 데이터 마이그레이션: varchar -> datetime 변환
            try {
                // 테이블 존재 여부 및 컬럼 타입 체크
                val checkColumnType = """
                    SELECT DATA_TYPE
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                    AND TABLE_NAME = 'trade_history'
                    AND COLUMN_NAME = 'order_time'
                """.trimIndent()

                val result = exec(checkColumnType) { rs ->
                    if (rs.next()) rs.getString(1) else null
                }

                // varchar 타입이면 마이그레이션 실행
                if (result == "varchar") {
                    println("[Migration] TradeHistory 테이블을 varchar -> datetime으로 마이그레이션 시작...")

                    // 1. 임시 테이블 생성 (새 스키마)
                    exec("""
                        CREATE TABLE IF NOT EXISTS trade_history_temp (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            ticker VARCHAR(20) NOT NULL,
                            order_no VARCHAR(50),
                            order_side VARCHAR(20) NOT NULL,
                            order_type VARCHAR(20) NOT NULL,
                            order_price DOUBLE NOT NULL,
                            order_quantity INT NOT NULL,
                            order_time DATETIME NOT NULL,
                            status VARCHAR(20) DEFAULT 'PENDING',
                            filled_quantity INT DEFAULT 0,
                            filled_price DOUBLE DEFAULT 0.0,
                            filled_time DATETIME,
                            t_value DOUBLE NOT NULL,
                            created_at DATETIME NOT NULL,
                            updated_at DATETIME NOT NULL
                        )
                    """.trimIndent())

                    // 2. 기존 데이터 변환하여 임시 테이블에 삽입
                    exec("""
                        INSERT INTO trade_history_temp
                        (id, ticker, order_no, order_side, order_type, order_price, order_quantity,
                         order_time, status, filled_quantity, filled_price, filled_time, t_value,
                         created_at, updated_at)
                        SELECT
                            id, ticker, order_no, order_side, order_type, order_price, order_quantity,
                            -- createdAt을 orderTime으로 사용 (시간 정보 유지)
                            -- ISO 8601 형식(2025-12-17T18:30:45.123)을 DATETIME으로 변환
                            CAST(REPLACE(SUBSTRING(created_at, 1, 19), 'T', ' ') AS DATETIME),
                            status, filled_quantity, filled_price,
                            -- filledTime 변환 (nullable)
                            CASE
                                WHEN filled_time IS NOT NULL AND filled_time != ''
                                THEN CAST(REPLACE(SUBSTRING(filled_time, 1, 19), 'T', ' ') AS DATETIME)
                                ELSE NULL
                            END,
                            t_value,
                            CAST(REPLACE(SUBSTRING(created_at, 1, 19), 'T', ' ') AS DATETIME),
                            CAST(REPLACE(SUBSTRING(updated_at, 1, 19), 'T', ' ') AS DATETIME)
                        FROM trade_history
                    """.trimIndent())

                    // 3. 기존 테이블 삭제
                    exec("DROP TABLE trade_history")

                    // 4. 임시 테이블을 원래 이름으로 rename
                    exec("RENAME TABLE trade_history_temp TO trade_history")

                    println("[Migration] TradeHistory 마이그레이션 완료")
                } else {
                    println("[Migration] TradeHistory 테이블이 이미 datetime 타입입니다. 마이그레이션 스킵.")
                }
            } catch (e: Exception) {
                println("[Migration] 마이그레이션 중 오류 발생: ${e.message}")
                println("[Migration] 기존 테이블이 없거나, 이미 마이그레이션되었을 수 있습니다.")
            }

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