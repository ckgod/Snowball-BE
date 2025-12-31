package com.ckgod.di

import com.ckgod.kis.config.KisMode
import com.ckgod.kis.websokets.ExecutionNotificationProcessor
import com.ckgod.kis.websokets.KisWebSocketsService
import com.ckgod.scheduler.SchedulerService
import org.koin.core.qualifier.named
import org.koin.dsl.module


val serviceModule = module {
    single {
        SchedulerService(
            syncStrategyUseCase = get(),
            generateOrdersUseCase = get()
        )
    }

    single<KisWebSocketsService>(named(KisMode.REAL)) {
        KisWebSocketsService(
            config = get(named(KisMode.REAL)),
            authService = get(named(KisMode.REAL)),
            httpClient = get(),
            processor = get()
        )
    }

    single<KisWebSocketsService>(named(KisMode.MOCK)) {
        KisWebSocketsService(
            config = get(named(KisMode.MOCK)),
            authService = get(named(KisMode.MOCK)),
            httpClient = get(),
            processor = get()
        )
    }

    single { ExecutionNotificationProcessor(get()) }
}