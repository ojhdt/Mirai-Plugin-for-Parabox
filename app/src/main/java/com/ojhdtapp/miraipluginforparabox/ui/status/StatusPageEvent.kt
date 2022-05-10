package com.ojhdtapp.miraipluginforparabox.ui.status

import android.graphics.Bitmap

sealed class StatusPageEvent {
    data class OnLoginClick(val accountNum: Long, val passwd:String) : StatusPageEvent()
    data class OnPicCaptchaConfirm(val captcha: String?): StatusPageEvent()
    data class OnSliderCaptchaConfirm(val url: String) : StatusPageEvent()
    data class OnUnsafeDeviceLoginVerifyConfirm(val url: String) : StatusPageEvent()
}

sealed class StatusPageUiEvent {

}