package com.ckgod.di

import com.ckgod.kis.KisApiClient
import com.ckgod.kis.api.KisApiService
import com.ckgod.kis.auth.KisAuthService
import com.ckgod.kis.config.KisMode
import org.koin.core.qualifier.named
import org.koin.dsl.module


val kisApiModule = module {
    single<KisAuthService>(named(KisMode.REAL)) {
        KisAuthService(
            config = get(named(KisMode.REAL)),
            client = get(),
            authTokenRepository = get()
        )
    }

    single<KisApiClient>(named(KisMode.REAL)) {
        KisApiClient(
            config = get(named(KisMode.REAL)),
            authService = get(named(KisMode.REAL)),
            client = get()
        )
    }

    single<KisApiService>(named(KisMode.REAL)) {
        KisApiService(get(named(KisMode.REAL)))
    }

    single<KisAuthService>(named(KisMode.MOCK)) {
        KisAuthService(
            config = get(named(KisMode.MOCK)),
            client = get(),
            authTokenRepository = get()
        )
    }

    single<KisApiClient>(named(KisMode.MOCK)) {
        KisApiClient(
            config = get(named(KisMode.MOCK)),
            authService = get(named(KisMode.MOCK)),
            client = get()
        )
    }

    single<KisApiService>(named(KisMode.MOCK)) {
        KisApiService(get(named(KisMode.MOCK)))
    }
}