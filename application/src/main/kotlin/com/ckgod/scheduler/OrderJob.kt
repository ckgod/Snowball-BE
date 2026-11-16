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
                val results = generateOrdersUseCase(ticker = null)

                if (results.isEmpty()) {
                    logger.warn("투자 중인 종목이 없습니다.")
                    return@runBlocking
                }

                logger.info("투자 중인 종목: ${results.size}개")

                results.forEach { result ->
                    logger.info("""
                        [${result.ticker}] 주문 생성:
                          - 현재가: $${"%.2f".format(result.currentPrice)}
                          - 매수 주문: ${result.buyOrders.size}개
                          - 매도 주문: ${result.sellOrders.size}개
                    """.trimIndent())

                    result.buyOrders.forEach { order ->
                        logger.info("  [BUY] ${order.type.name} ${order.quantity}주 @$${"%.2f".format(order.price)}")
                    }

                    result.sellOrders.forEach { order ->
                        val priceStr = if (order.type.name == "MOC") "MOC" else "$${"%.2f".format(order.price)}"
                        logger.info("  [SELL] ${order.type.name} ${order.quantity}주 @${priceStr}")
                    }
                }

                logger.info("=== [오후 6시] 주문 생성 완료 (${results.size}개 종목) ===")

            } catch (e: Exception) {
                logger.error("주문 생성 중 오류 발생", e)
            }
        }
    }
}
