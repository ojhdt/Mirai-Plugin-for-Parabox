package com.ojhdtapp.miraipluginforparabox.data.repository

import com.ojhdtapp.miraipluginforparabox.data.local.SecretDao
import com.ojhdtapp.miraipluginforparabox.domain.model.Secret
import com.ojhdtapp.miraipluginforparabox.domain.repository.MainRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class MainRepositoryImpl @Inject constructor(
    private val dao: SecretDao
) : MainRepository {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAccountListFlow(): Flow<List<Secret>> {
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
                emit(it.map { it.toSecret() })
                emit(it.map { it.toAvatarDownloadedSecret() })
            }
        }
    }

    override suspend fun getSelectedAccount(): Secret? {
        return dao.getSelectedSecret()?.toSecret()
    }

    override suspend fun addNewAccount(secret: Secret) {
        dao.insertSecret(secret.toSecretsEntity())
    }

    override suspend fun deleteAccount(secret: Secret) {
        dao.deleteSecret(secret.toSecretsEntity())
    }

    override suspend fun addAllAccounts(secretList: List<Secret>) {
        dao.insertAllSecrets(secretList.map { it.toSecretsEntity() })
    }

}