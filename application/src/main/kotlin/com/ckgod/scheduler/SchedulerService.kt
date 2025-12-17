package com.ckgod.scheduler

import com.ckgod.domain.usecase.GenerateOrdersUseCase
import com.ckgod.domain.usecase.SyncStrategyUseCase
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory
import org.slf4j.LoggerFactory

/**
 * 스케줄러 서비스
 *
 * DB에 등록된 모든 종목을 대상으로:
 * - 오전 10시: 정산 (SyncJob)
 * - 오후 6시: 주문 (OrderJob)
 */
class SchedulerService(
    private val syncStrategyUseCase: SyncStrategyUseCase,
    private val generateOrdersUseCase: GenerateOrdersUseCase
) {
    private val logger = LoggerFactory.getLogger(SchedulerService::class.java)
    private lateinit var scheduler: Scheduler

    fun start() {
        logger.info("스케줄러 시작 중...")

        scheduler = StdSchedulerFactory.getDefaultScheduler()

        scheduler.setJobFactory { bundle, _ ->
            when {
                bundle.jobDetail.key.name.startsWith("syncJob") -> SyncJob(syncStrategyUseCase)
                bundle.jobDetail.key.name.startsWith("orderJob") -> OrderJob(generateOrdersUseCase)
                else -> throw IllegalArgumentException("Unknown job: ${bundle.jobDetail.key.name}")
            }
        }

        // TODO 장이 안열리는 주말은 제외
        // 1. 정산 Job (오전 10시)
        scheduleSyncJob()

        // 2. 주문 Job (오후 6시)
        scheduleOrderJob()

        scheduler.start()
        logger.info("스케줄러 시작 완료")
        logger.info("  - 정산: 매일 오전 10시 (Asia/Seoul)")
        logger.info("  - 주문: 매일 오후 6시 (Asia/Seoul)")
    }

    private fun scheduleSyncJob() {
        val jobDetail = JobBuilder.newJob(SyncJob::class.java)
            .withIdentity("syncJob", "trading")
            .build()

        val trigger = TriggerBuilder.newTrigger()
            .withIdentity("syncTrigger", "trading")
            .withSchedule(
                CronScheduleBuilder.dailyAtHourAndMinute(10, 0)
                    .inTimeZone(java.util.TimeZone.getTimeZone("Asia/Seoul"))
            )
            .build()

        scheduler.scheduleJob(jobDetail, trigger)
    }

    private fun scheduleOrderJob() {
        val jobDetail = JobBuilder.newJob(OrderJob::class.java)
            .withIdentity("orderJob", "trading")
            .build()

        val trigger = TriggerBuilder.newTrigger()
            .withIdentity("orderTrigger", "trading")
            .withSchedule(
                CronScheduleBuilder.dailyAtHourAndMinute(18, 0)
                    .inTimeZone(java.util.TimeZone.getTimeZone("Asia/Seoul"))
            )
            .build()

        scheduler.scheduleJob(jobDetail, trigger)
    }

    fun stop() {
        if (::scheduler.isInitialized && !scheduler.isShutdown) {
            logger.info("스케줄러 중지 중...")
            scheduler.shutdown(true)
            logger.info("스케줄러 중지 완료")
        }
    }

    /**
     * 테스트용: 즉시 정산 실행
     */
    fun runSyncNow() {
        logger.info("즉시 정산 실행")
        val jobDetail = JobBuilder.newJob(SyncJob::class.java)
            .withIdentity("syncJobNow", "manual")
            .build()

        val trigger = TriggerBuilder.newTrigger()
            .withIdentity("syncTriggerNow", "manual")
            .startNow()
            .build()

        scheduler.scheduleJob(jobDetail, trigger)
    }

    /**
     * 테스트용: 즉시 주문 실행
     */
    fun runOrderNow() {
        logger.info("즉시 주문 실행")
        val jobDetail = JobBuilder.newJob(OrderJob::class.java)
            .withIdentity("orderJobNow", "manual")
            .build()

        val trigger = TriggerBuilder.newTrigger()
            .withIdentity("orderTriggerNow", "manual")
            .startNow()
            .build()

        scheduler.scheduleJob(jobDetail, trigger)
    }
}
