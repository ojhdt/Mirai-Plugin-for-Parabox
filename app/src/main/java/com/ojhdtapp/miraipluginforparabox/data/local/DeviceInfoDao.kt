package com.ojhdtapp.miraipluginforparabox.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ojhdtapp.miraipluginforparabox.data.local.entity.DeviceInfoEntity

@Dao
interface DeviceInfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDeviceInfo(value: DeviceInfoEntity)

    @Query("SELECT * FROM deviceinfoentity LIMIT 1")
    suspend fun getDeviceInfo() : DeviceInfoEntity?
}