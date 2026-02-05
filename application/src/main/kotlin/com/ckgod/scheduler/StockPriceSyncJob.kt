package com.ckgod.scheduler

import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * 주식 가격 동기화 Job (매일 오전 5시 실행)
 *
 * Python 스크립트를 실행하여 Yahoo Finance에서 최신 가격 데이터를 가져와 DB에 저장
 * 미국 동부 시간 기준 오전 5시 (장 시작 전)
 */
class StockPriceSyncJob : Job {

    private val logger = LoggerFactory.getLogger(StockPriceSyncJob::class.java)

    override fun execute(context: JobExecutionContext?) {
        logger.info("=== [오전 5시] 주식 가격 동기화 시작 ===")

        try {
            val scriptPath = System.getenv("STOCK_PRICE_SYNC_SCRIPT")
                ?: run {
                    val currentDir = System.getProperty("user.dir")
                    val scriptFile = java.io.File(currentDir, "scripts/sync_stock_prices.py")

                    if (scriptFile.exists()) {
                        scriptFile.absolutePath
                    } else {
                        val parentScript = java.io.File(currentDir).parentFile?.let {
                            java.io.File(it, "scripts/sync_stock_prices.py")
                        }
                        parentScript?.absolutePath ?: scriptFile.absolutePath
                    }
                }

            logger.info("작업 디렉토리: ${System.getProperty("user.dir")}")
            logger.info("Python 스크립트 경로: $scriptPath")

            val process = ProcessBuilder(
                "python3",
                scriptPath,
                "daily"
            ).apply {
                environment().putAll(
                    mapOf(
                        "DB_HOST" to (System.getenv("DB_HOST") ?: "localhost"),
                        "DB_PORT" to (System.getenv("DB_PORT") ?: "3306"),
                        "DB_USER" to (System.getenv("DB_USER") ?: "root"),
                        "DB_PASSWORD" to (System.getenv("DB_PASSWORD") ?: ""),
                        "DB_NAME" to (System.getenv("DB_NAME") ?: "snowball")
                    )
                )
                redirectErrorStream(true)
            }.start()

            // 스크립트 실행 로그 실시간 출력
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                logger.info("[Python] $line")
            }

            // 프로세스 종료 대기 (최대 10분)
            val finished = process.waitFor(10, TimeUnit.MINUTES)

            if (!finished) {
                logger.error("Python 스크립트가 10분 내에 완료되지 않아 강제 종료합니다.")
                process.destroy()
                return
            }

            val exitCode = process.exitValue()
            if (exitCode == 0) {
                logger.info("=== [오전 5시] 주식 가격 동기화 완료 ===")
            } else {
                logger.error("Python 스크립트가 오류와 함께 종료되었습니다. Exit code: $exitCode")
            }

        } catch (e: Exception) {
            logger.error("주식 가격 동기화 중 오류 발생", e)
        }
    }
}
