package com.ojhdtapp.miraipluginforparabox.ui.status

import android.graphics.Bitmap

sealed class StatusPageEvent {
    data class OnLoginClick(val accountNum: Long, val passwd:String) : StatusPageEvent()
    object OnKillClick : StatusPageEvent()
    data class OnPicCaptchaConfirm(val captcha: String?): StatusPageEvent()
    data class OnSliderCaptchaConfirm(val ticket: String) : StatusPageEvent()
    object OnUnsafeDeviceLoginVerifyConfirm : StatusPageEvent()
}

sealed class StatusPageUiEvent {

}