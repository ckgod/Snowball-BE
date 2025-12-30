package com.ckgod.di

import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureKoin() {
    install(Koin) {
        slf4jLogger()
        modules(listOf(
            module { single { this@configureKoin } },
            httpClientModule,
            kisConfigModule,
            repositoryModule,
            kisApiModule,
            useCaseModule,
            serviceModule
        ))
    }
}

