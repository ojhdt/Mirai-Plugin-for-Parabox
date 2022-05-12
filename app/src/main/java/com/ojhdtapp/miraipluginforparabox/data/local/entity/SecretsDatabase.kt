package com.ojhdtapp.miraipluginforparabox.data.local.entity

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ojhdtapp.miraipluginforparabox.data.local.SecretsDao

@Database(entities = [SecretsEntity::class], version = 1)
abstract class SecretsDatabase : RoomDatabase() {
    abstract val dao: SecretsDao
}