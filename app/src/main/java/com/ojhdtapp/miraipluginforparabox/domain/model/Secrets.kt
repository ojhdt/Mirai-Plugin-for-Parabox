package com.ojhdtapp.miraipluginforparabox.domain.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.ojhdtapp.miraipluginforparabox.data.local.entity.SecretsEntity
import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.delay
import net.mamoe.mirai.Bot

data class Secrets(
    val account: Long,
    val password: String,
    val selected: Boolean = false,
    val avatarUrl: String? = null,
    var bitmap: Bitmap? = null
) {
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

    fun toSecretsEntity() =
        SecretsEntity(
            account, password, selected ,avatarUrl
        )
}
