package com.ckgod.scheduler

import com.ckgod.domain.usecase.GenerateOrdersUseCase
import kotlinx.coroutines.runBlocking
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory

/**
 * 주문 Job (오후 6시 실행)
 *
 * DB의 모든 종목에 대해 주문 생성
 */
class OrderJob(
    private val generateOrdersUseCase: GenerateOrdersUseCase
) : Job {

    private val logger = LoggerFactory.getLogger(OrderJob::class.java)

    override fun execute(context: JobExecutionContext) {
        logger.info("=== [오후 6시] 주문 생성 시작 ===")

        runBlocking {
            try {
                // UseCase 호출 (ticker=null → 전체 종목)
                val results = generateOrdersUseCase(ticker = null)

                if (results.isEmpty()) {
                    logger.warn("투자 중인 종목이 없습니다.")
                    return@runBlocking
                }

                logger.info("투자 중인 종목: ${results.size}개")

                // 결과 로깅 및 주문 전송
                results.forEach { result ->
                    logger.info("""
                        [${result.ticker}] 주문 가격 계산:
                          - 현재가: $${result.currentPrice}
                          - 매수: ${result.buyQuantity}주 @$${"%.2f".format(result.buyPrice)}
                          - 매도: @$${"%.2f".format(result.sellPrice)}
                          - 별%: ${result.targetRate}%
                    """.trimIndent())

                    // TODO: 실제 한투 API 주문 전송
                    if (result.buyQuantity > 0) {
                        logger.info("[${result.ticker}] 매수 주문 전송 완료 (로그만)")
                    }

                    if (result.sellPrice > 0) {
                        logger.info("[${result.ticker}] 매도 주문 전송 완료 (로그만)")
                    }
                }

                logger.info("=== [오후 6시] 주문 생성 완료 (${results.size}개 종목) ===")

            } catch (e: Exception) {
                logger.error("주문 생성 중 오류 발생", e)
            }
        }
    }
}
