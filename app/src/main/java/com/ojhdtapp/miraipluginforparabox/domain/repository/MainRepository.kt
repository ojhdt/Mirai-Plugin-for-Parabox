package com.ojhdtapp.miraipluginforparabox.domain.repository

import com.ojhdtapp.miraipluginforparabox.domain.model.Secrets
import kotlinx.coroutines.flow.Flow

interface MainRepository {
    fun getAccountListFlow() : Flow<List<Secrets>>

    suspend fun addNewAccount(secrets: Secrets)

    suspend fun deleteAccount(secrets: Secrets)

    suspend fun addAllAccounts(secretList: List<Secrets>)
}