package com.ojhdtapp.miraipluginforparabox.data.local.entity

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ojhdtapp.miraipluginforparabox.domain.model.Secret
import io.ktor.client.*
import io.ktor.client.request.*

@Entity
class SecretEntity(
    @PrimaryKey var account: Long,
    var password: String,
    var selected: Boolean = false,
    var avatarUrl: String? = null
) {
    fun toSecret(): Secret = Secret(
        account, password, selected ,avatarUrl
    )

    suspend fun toAvatarDownloadedSecret(): Secret = Secret(
        account = account,
        password = password,
        selected = selected,
        avatarUrl = avatarUrl,
        bitmap = downloadAvatar()
    )
    private suspend fun downloadAvatar(): Bitmap? =
        avatarUrl?.let {
            try {
                HttpClient().get<ByteArray>(it).let { avatarData ->
                    BitmapFactory.decodeByteArray(avatarData, 0, avatarData.size)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
}