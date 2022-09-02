package com.ojhdtapp.miraipluginforparabox.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class MiraiMessageEntity(
    @PrimaryKey val messageId: Long,
    val miraiCode: String,
    val json: String,
)