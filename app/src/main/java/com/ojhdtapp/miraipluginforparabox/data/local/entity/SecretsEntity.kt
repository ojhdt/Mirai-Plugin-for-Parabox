package com.ojhdtapp.miraipluginforparabox.data.local.entity

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ojhdtapp.miraipluginforparabox.domain.model.Secrets
import io.ktor.client.*
import io.ktor.client.request.*

@Entity
class SecretsEntity(
    @PrimaryKey var account: Long,
    var password: String,
    var selected: Boolean = false,
    var avatarUrl: String? = null
) {
    fun toSecrets(): Secrets = Secrets(
        account, password, selected ,avatarUrl
    )

    suspend fun toAvatarDownloadedSecrets(): Secrets = Secrets(
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