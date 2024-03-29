package com.ojhdtapp.miraipluginforparabox.data.repository

import android.util.Log
import com.ojhdtapp.miraipluginforparabox.data.local.DeviceInfoDao
import com.ojhdtapp.miraipluginforparabox.data.local.MiraiMessageDao
import com.ojhdtapp.miraipluginforparabox.data.local.SecretDao
import com.ojhdtapp.miraipluginforparabox.data.local.entity.MiraiMessageEntity
import com.ojhdtapp.miraipluginforparabox.data.local.entity.toDeviceInfoEntity
import com.ojhdtapp.miraipluginforparabox.domain.model.Secret
import com.ojhdtapp.miraipluginforparabox.domain.repository.MainRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import net.mamoe.mirai.utils.DeviceInfo
import javax.inject.Inject

class MainRepositoryImpl @Inject constructor(
    private val secretDao: SecretDao,
    private val deviceInfoDao: DeviceInfoDao,
    private val miraiMessageDao: MiraiMessageDao,

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
        return secretDao.getAllSecretsFlow().flatMapLatest {
            flow {
                emit(it.map { it.toSecret() })
//                emit(it.map { it.toAvatarDownloadedSecret() })
            }
        }
    }

    override suspend fun getSelectedAccount(): Secret? {
        return secretDao.getSelectedSecret()?.toSecret()
    }

    override suspend fun addNewAccount(secret: Secret) {
        secretDao.insertSecret(secret.toSecretsEntity())
    }

    override suspend fun deleteAccount(secret: Secret) {
        secretDao.deleteSecret(secret.toSecretsEntity())
    }

    override suspend fun addAllAccounts(secretList: List<Secret>) {
        secretDao.insertAllSecrets(secretList.map { it.toSecretsEntity() })
    }

    override fun insertDeviceInfo(value: DeviceInfo) {
        deviceInfoDao.insertDeviceInfo(value.toDeviceInfoEntity())
    }

    override suspend fun getDeviceInfo(): DeviceInfo? {
        return deviceInfoDao.getDeviceInfo()?.toMiraiDeviceInfo()
    }

    override fun insertMiraiMessage(message: MiraiMessageEntity) {
        miraiMessageDao.insertMessage(message)
    }

    override suspend fun getMiraiMessageById(messageId: Long): MiraiMessageEntity? {
        return miraiMessageDao.getMessageById(messageId)
    }

}