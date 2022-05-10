package com.ojhdtapp.miraipluginforparabox.domain.util

import android.graphics.Bitmap

sealed class LoginResource{
    object None : LoginResource()
    data class PicCaptcha(val captchaBitMap: Bitmap) : LoginResource()
    data class SliderCaptcha(val url:String): LoginResource()
    data class UnsafeDeviceLoginVerify(val url: String) : LoginResource()
}
