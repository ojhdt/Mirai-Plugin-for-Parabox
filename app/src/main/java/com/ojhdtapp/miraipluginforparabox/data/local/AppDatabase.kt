package com.ojhdtapp.miraipluginforparabox.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ojhdtapp.miraipluginforparabox.data.local.entity.DeviceInfoEntity
import com.ojhdtapp.miraipluginforparabox.data.local.entity.MiraiMessageEntity
import com.ojhdtapp.miraipluginforparabox.data.local.entity.SecretEntity

@Database(entities = [SecretEntity::class, DeviceInfoEntity::class, MiraiMessageEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract val secretDao: SecretDao
    abstract val deviceInfoDao : DeviceInfoDao
    abstract val miraiMessageDao: MiraiMessageDao
}