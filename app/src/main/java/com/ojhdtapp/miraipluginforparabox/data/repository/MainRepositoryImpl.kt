package com.ojhdtapp.miraipluginforparabox.data.repository

import com.ojhdtapp.miraipluginforparabox.data.local.SecretsDao
import com.ojhdtapp.miraipluginforparabox.domain.model.Secrets
import com.ojhdtapp.miraipluginforparabox.domain.repository.MainRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class MainRepositoryImpl @Inject constructor(
    private val dao: SecretsDao
) : MainRepository {
    override fun getAccountListFlow(): Flow<List<Secrets>> {
        return flow {
//            emit(emptyList())
            val accounts = dao.getAllSecrets()
            emit(accounts.map {
                it.toSecrets()
            })
            emit(accounts.map {
                it.toAvatarDownloadedSecrets()
            })
        }
    }

}