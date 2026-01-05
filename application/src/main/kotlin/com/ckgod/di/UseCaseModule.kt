package com.ckgod.di

import com.ckgod.domain.usecase.GenerateOrdersUseCase
import com.ckgod.domain.usecase.GetAccountStatusUseCase
import com.ckgod.domain.usecase.GetCurrentPriceUseCase
import com.ckgod.domain.usecase.SyncStrategyUseCase
import org.koin.dsl.module

val useCaseModule = module {
    single<GetAccountStatusUseCase> { GetAccountStatusUseCase(get()) }

    single<GetCurrentPriceUseCase> { GetCurrentPriceUseCase(get()) }

    single<SyncStrategyUseCase> { SyncStrategyUseCase(get(), get(), get(), get()) }

    single<GenerateOrdersUseCase> { GenerateOrdersUseCase(get(), get(), get(), get()) }
}