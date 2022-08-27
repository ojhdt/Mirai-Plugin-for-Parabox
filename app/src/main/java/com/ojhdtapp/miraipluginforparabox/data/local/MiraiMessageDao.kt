package com.ojhdtapp.miraipluginforparabox.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ojhdtapp.miraipluginforparabox.data.local.entity.MiraiMessageEntity

@Dao
interface MiraiMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessage(miraiMessage: MiraiMessageEntity)

    @Query("SELECT * FROM miraimessageentity WHERE messageId = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: Long) : MiraiMessageEntity?
}