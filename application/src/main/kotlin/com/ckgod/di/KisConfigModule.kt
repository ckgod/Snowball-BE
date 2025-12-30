package com.ckgod.di

import com.ckgod.kis.config.KisConfig
import com.ckgod.kis.config.KisMode
import io.ktor.server.application.Application
import org.koin.core.qualifier.named
import org.koin.dsl.module

val kisConfigModule = module {
    single(named(KisMode.REAL)) {
        val config = get<Application>().environment.config
        KisConfig(
            mode = KisMode.REAL,
            baseUrl = config.property("kis.real.baseUrl").getString().trim(),
            appKey = config.property("kis.real.appKey").getString().trim(),
            appSecret = config.property("kis.real.appSecret").getString().trim(),
            accountNo = config.property("kis.real.accountNo").getString().trim(),
            accountCode = config.property("kis.real.accountCode").getString().trim(),
            userId = config.property("kis.userId").getString().trim()
        )
    }

    single(named(KisMode.MOCK)) {
        val config = get<Application>().environment.config
        KisConfig(
            mode = KisMode.MOCK,
            baseUrl = config.property("kis.mock.baseUrl").getString().trim(),
            appKey = config.property("kis.mock.appKey").getString().trim(),
            appSecret = config.property("kis.mock.appSecret").getString().trim(),
            accountNo = config.property("kis.mock.accountNo").getString().trim(),
            accountCode = config.property("kis.mock.accountCode").getString().trim(),
            userId = config.property("kis.userId").getString().trim()
        )
    }
}