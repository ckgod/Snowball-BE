package com.ckgod.domain.usecase

import com.ckgod.domain.model.AccountStatus
import com.ckgod.domain.repository.AccountRepository

class GetAccountStatusUseCase(
    val repository: AccountRepository
) {
    suspend operator fun invoke(isRealMode: Boolean): AccountStatus {
        return repository.getAccountBalance()
    }
}