package com.ckgod.di

import com.ckgod.database.InvestmentStatusRepositoryImpl
import com.ckgod.database.TradeHistoryRepositoryImpl
import com.ckgod.database.auth.AuthTokenRepository
import com.ckgod.domain.repository.AccountRepository
import com.ckgod.domain.repository.ExecutionRepository
import com.ckgod.domain.repository.InvestmentStatusRepository
import com.ckgod.domain.repository.StockRepository
import com.ckgod.domain.repository.TradeHistoryRepository
import com.ckgod.kis.config.KisMode
import com.ckgod.kis.repository.AccountRepositoryImpl
import com.ckgod.kis.repository.ExecutionRepositoryImpl
import com.ckgod.kis.repository.StockRepositoryImpl
import org.koin.core.qualifier.named
import org.koin.dsl.module

val repositoryModule = module {
    single<InvestmentStatusRepository> { InvestmentStatusRepositoryImpl() }

    single<TradeHistoryRepository> { TradeHistoryRepositoryImpl() }

    single { AuthTokenRepository() }

    single<StockRepository> { StockRepositoryImpl(get(named(KisMode.REAL))) }

    single<AccountRepository> { AccountRepositoryImpl(get(named(KisMode.REAL))) }

    single<ExecutionRepository> { ExecutionRepositoryImpl(get(named(KisMode.REAL))) }
}