package com.ojhdtapp.miraipluginforparabox.domain.repository

import com.ojhdtapp.miraipluginforparabox.domain.model.Secret
import kotlinx.coroutines.flow.Flow

interface MainRepository {
    fun getAccountListFlow() : Flow<List<Secret>>

    suspend fun getSelectedAccount() : Secret?

    suspend fun addNewAccount(secret: Secret)

    suspend fun deleteAccount(secret: Secret)

    suspend fun addAllAccounts(secretList: List<Secret>)
}