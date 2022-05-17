package com.ojhdtapp.miraipluginforparabox.domain.repository

import com.ojhdtapp.miraipluginforparabox.domain.model.Secret
import kotlinx.coroutines.flow.Flow

interface MainRepository {
    fun getAccountListFlow() : Flow<List<Secret>>

    suspend fun addNewAccount(secrets: Secret)

    suspend fun deleteAccount(secrets: Secret)

    suspend fun addAllAccounts(secretList: List<Secret>)
}