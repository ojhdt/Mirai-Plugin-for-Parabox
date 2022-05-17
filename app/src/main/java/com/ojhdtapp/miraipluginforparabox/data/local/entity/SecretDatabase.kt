package com.ojhdtapp.miraipluginforparabox.data.local.entity

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ojhdtapp.miraipluginforparabox.data.local.SecretDao

@Database(entities = [SecretEntity::class], version = 1, exportSchema = false)
abstract class SecretDatabase : RoomDatabase() {
    abstract val dao: SecretDao
}