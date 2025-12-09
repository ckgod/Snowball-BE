package com.ckgod.infrastructure.scheduler

import org.quartz.CronScheduleBuilder
import org.quartz.Job
import org.quartz.JobBuilder
import org.quartz.Scheduler
import org.quartz.TriggerBuilder
import org.quartz.impl.StdSchedulerFactory
import org.slf4j.LoggerFactory

object QuartzSchedulerManager {
    private val logger = LoggerFactory.getLogger(QuartzSchedulerManager::class.java)
    private val scheduler: Scheduler = StdSchedulerFactory.getDefaultScheduler()

    fun start() {
        if (!scheduler.isStarted) {
            scheduler.start()
            logger.info("Quartz Scheduler started.")
        }
    }

    fun shutdown() {
        if (scheduler.isStarted) {
            scheduler.shutdown()
            logger.info("Quartz Scheduler shutdown.")
        }
    }

    fun scheduleJob(
        jobClass: Class<out Job>,
        jobName: String,
        groupName: String,
        cronExpression: String,
        jobData: Map<String, Any>
    ) {
        val jobDetail = JobBuilder.newJob(jobClass)
            .withIdentity(jobName, groupName)
            .build()

        jobData.forEach { (key, value) ->
            jobDetail.jobDataMap[key] = value
        }

        val trigger = TriggerBuilder.newTrigger()
            .withIdentity("$jobName-Trigger", groupName)
            .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
            .build()

        scheduler.scheduleJob(jobDetail, trigger)
        logger.info("Scheduled job '$jobName' with cron '$cronExpression'")
    }
}
