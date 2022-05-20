package com.ojhdtapp.miraipluginforparabox.ui.status

import android.graphics.Bitmap

sealed class StatusPageEvent {
    object OnServiceStart: StatusPageEvent()
    object OnServiceStop: StatusPageEvent()
    object OnServiceForceStop: StatusPageEvent()

    data class OnLoginResourceConfirm(val res: String, val timestamp: Long) : StatusPageEvent()

//    data class OnPicCaptchaConfirm(val captcha: String?, val timestamp: Long): StatusPageEvent()
//    data class OnSliderCaptchaConfirm(val ticket: String, val timestamp: Long) : StatusPageEvent()
//    data class OnUnsafeDeviceLoginVerifyConfirm(val timestamp: Long) : StatusPageEvent()
    data class OnShowToast(val message: String): StatusPageEvent()
}

sealed class StatusPageUiEvent {
    data class ShowSnackBar(val message: String) : StatusPageUiEvent()
}