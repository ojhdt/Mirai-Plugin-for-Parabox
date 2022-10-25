package com.ojhdtapp.miraipluginforparabox.domain.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.ojhdtapp.miraipluginforparabox.data.local.entity.SecretEntity

data class Secret(
    var account: Long,
    var password: String,
    var selected: Boolean = false,
    var avatarUrl: String? = null,
    var bitmap: Bitmap? = null
) {
//    suspend fun toAvatarDownloadedSecret(): Secret = Secret(
//        account = account,
//        password = password,
//        selected = selected,
//        avatarUrl = avatarUrl,
//        bitmap = downloadAvatar()
//    )

//    private suspend fun downloadAvatar(): Bitmap? =
//        avatarUrl?.let {
//            try {
//                HttpClient().get<ByteArray>(it).let { avatarData ->
//                    BitmapFactory.decodeByteArray(avatarData, 0, avatarData.size)
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//                null
//            }
//        }

    fun toSecretsEntity() =
        SecretEntity(
            account, password, selected ,avatarUrl
        )
}
