package com.ckgod.di

import com.ckgod.domain.usecase.BacktestUseCase
import com.ckgod.domain.usecase.GenerateOrdersUseCase
import com.ckgod.domain.usecase.GetCurrentPriceUseCase
import com.ckgod.domain.usecase.GetStockPriceHistoryUseCase
import com.ckgod.domain.usecase.SyncStrategyUseCase
import org.koin.dsl.module

val useCaseModule = module {
    single<GetCurrentPriceUseCase> { GetCurrentPriceUseCase(get()) }

    single<SyncStrategyUseCase> { SyncStrategyUseCase(get(), get(), get(), get()) }

    single<GenerateOrdersUseCase> { GenerateOrdersUseCase(get(), get(), get(), get()) }

    single<GetStockPriceHistoryUseCase> { GetStockPriceHistoryUseCase(get()) }

    single<BacktestUseCase> { BacktestUseCase(get()) }
}