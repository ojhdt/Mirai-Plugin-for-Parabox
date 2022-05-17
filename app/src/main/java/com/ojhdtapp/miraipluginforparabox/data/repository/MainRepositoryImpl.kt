package com.ojhdtapp.miraipluginforparabox.data.repository

import com.ojhdtapp.miraipluginforparabox.data.local.SecretsDao
import com.ojhdtapp.miraipluginforparabox.domain.model.Secrets
import com.ojhdtapp.miraipluginforparabox.domain.repository.MainRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class MainRepositoryImpl @Inject constructor(
    private val dao: SecretsDao
) : MainRepository {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAccountListFlow(): Flow<List<Secrets>> {
//        return flow {
//            val accounts = dao.getAllSecrets()
//            emit(accounts.map {
//                it.toSecrets()
//            })
//            emit(accounts.map {
//                it.toAvatarDownloadedSecrets()
//            })
//        }

//        return dao.getAllSecretsFlow()
//            .map { list ->
//                list.map { it.toSecrets() }
//            }
        return dao.getAllSecretsFlow().flatMapLatest {
            flow {
                emit(it.map { it.toSecrets() })
                emit(it.map { it.toAvatarDownloadedSecrets() })
            }
        }
    }

    override suspend fun addNewAccount(secrets: Secrets) {
        dao.insertSecrets(secrets.toSecretsEntity())
    }

    override suspend fun deleteAccount(secrets: Secrets) {
        dao.deleteSecrets(secrets.toSecretsEntity())
    }

    override suspend fun addAllAccounts(secretList: List<Secrets>) {
        dao.insertAllSecrets(secretList.map { it.toSecretsEntity() })
    }

}