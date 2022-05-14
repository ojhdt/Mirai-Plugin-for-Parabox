package com.ojhdtapp.miraipluginforparabox.domain.util

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

sealed class LoginResource : Parcelable {
    @Parcelize
    object None: LoginResource()
    @Parcelize data class PicCaptcha(val captchaBitMap: Bitmap) : LoginResource()
    @Parcelize data class SliderCaptcha(val url:String): LoginResource()
    @Parcelize data class UnsafeDeviceLoginVerify(val url: String) : LoginResource()
}

object LoginResourceType{
    const val None = 0
    const val PicCaptcha = 1
    const val SliderCaptcha = 2
    const val UnsafeDeviceLoginVerify = 3
}