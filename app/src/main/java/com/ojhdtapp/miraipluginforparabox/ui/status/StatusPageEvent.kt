package com.ojhdtapp.miraipluginforparabox.ui.status

import android.graphics.Bitmap

sealed class StatusPageEvent {
    object OnServiceStart : StatusPageEvent()
    object OnServiceStop : StatusPageEvent()
    object OnServiceForceStop : StatusPageEvent()
    data class OnLoginResourceConfirm(val res: String, val timestamp: Long) : StatusPageEvent()
    object OnRequestIgnoreBatteryOptimizations : StatusPageEvent()
    data class OnShowToast(val message: String) : StatusPageEvent()
    data class OnLaunchBrowser(val url: String) : StatusPageEvent()
}

sealed class StatusPageUiEvent {
    data class ShowSnackBar(val message: String) : StatusPageUiEvent()
}