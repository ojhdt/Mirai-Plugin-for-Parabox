package com.ojhdtapp.miraipluginforparabox.domain.util

import android.graphics.Bitmap
import android.os.Parcelable
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxMetadata
import kotlinx.parcelize.Parcelize

sealed interface LoginResource : Parcelable {
    @Parcelize
    object None : LoginResource
    @Parcelize
    data class PicCaptcha(val captchaBitMap: Bitmap, val metadata: ParaboxMetadata) :
        LoginResource

    @Parcelize
    data class SliderCaptcha(val url: String, val metadata: ParaboxMetadata) :
        LoginResource

    @Parcelize
    data class UnsafeDeviceLoginVerify(val url: String, val metadata: ParaboxMetadata) :
        LoginResource

    @Parcelize
    data class Sms(val number: String, val metadata: ParaboxMetadata): LoginResource

    @Parcelize
    data class Fallback(val url: String, val metadata: ParaboxMetadata): LoginResource
}

object LoginResourceType {
    const val None = 0
    const val PicCaptcha = 1
    const val SliderCaptcha = 2
    const val UnsafeDeviceLoginVerify = 3
}