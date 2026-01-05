package com.ckgod.di

import com.ckgod.scheduler.SchedulerService
import org.koin.dsl.module


val serviceModule = module {
    single {
        SchedulerService(
            syncStrategyUseCase = get(),
            generateOrdersUseCase = get()
        )
    }
}