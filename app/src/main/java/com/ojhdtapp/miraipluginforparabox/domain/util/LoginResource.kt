package com.ojhdtapp.miraipluginforparabox.domain.util

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

sealed class LoginResource(open val timestamp: Long) : Parcelable {
    @Parcelize
    object None : LoginResource(-1L)
    @Parcelize
    data class PicCaptcha(val captchaBitMap: Bitmap, override val timestamp: Long) :
        LoginResource(timestamp)

    @Parcelize
    data class SliderCaptcha(val url: String, override val timestamp: Long) :
        LoginResource(timestamp)

    @Parcelize
    data class UnsafeDeviceLoginVerify(val url: String, override val timestamp: Long) :
        LoginResource(timestamp)
}

object LoginResourceType {
    const val None = 0
    const val PicCaptcha = 1
    const val SliderCaptcha = 2
    const val UnsafeDeviceLoginVerify = 3
}