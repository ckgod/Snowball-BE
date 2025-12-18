package com.ckgod.scheduler

import com.ckgod.domain.usecase.SyncStrategyUseCase
import kotlinx.coroutines.runBlocking
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory

/**
 * 정산 Job (오전 7시 실행)
 *
 * DB의 모든 종목을 정산
 */
class SyncJob(
    private val syncStrategyUseCase: SyncStrategyUseCase
) : Job {

    private val logger = LoggerFactory.getLogger(SyncJob::class.java)

    override fun execute(context: JobExecutionContext) {
        logger.info("=== [오전 7시] 정산 시작 ===")

        runBlocking {
            try {
                val results = syncStrategyUseCase(ticker = null)

                if (results.isEmpty()) {
                    logger.warn("투자 중인 종목이 없습니다.")
                    return@runBlocking
                }

                logger.info("투자 중인 종목: ${results.size}개")

                results.forEach { result ->
                    logger.info("""
                        [${result.ticker}] 정산 완료:
                          - T값: ${result.before.tValue} → ${result.after.tValue}
                          - 1회매수금: ${result.before.oneTimeAmount} → ${result.after.oneTimeAmount}
                          - 별%: ${result.after.starPercent}%
                          - 평단가: ${result.after.avgPrice}
                          - 일일수익: ${result.dailyProfit}
                    """.trimIndent())
                }

                logger.info("=== [오전 7시] 정산 완료 (${results.size}개 종목) ===")

            } catch (e: Exception) {
                logger.error("정산 중 오류 발생", e)
            }
        }
    }
}
