package com.ojhdtapp.miraipluginforparabox.ui.status

import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxMetadata


sealed class StatusPageEvent {
    object OnServiceStart : StatusPageEvent()
    object OnServiceStop : StatusPageEvent()
    object OnServiceForceStop : StatusPageEvent()
    object OnServiceLogin : StatusPageEvent()
    data class OnLoginResourceConfirm(val res: String, val metadata: ParaboxMetadata) : StatusPageEvent()
    data class OnDeviceVerificationSmsConfirm(val res: String, val metadata: ParaboxMetadata): StatusPageEvent()
    data class OnDeviceVerificationFallbackConfirm(val metadata: ParaboxMetadata): StatusPageEvent()
    object OnRequestIgnoreBatteryOptimizations : StatusPageEvent()
    data class OnShowToast(val message: String) : StatusPageEvent()
    data class OnLaunchBrowser(val url: String) : StatusPageEvent()
    object LaunchMainApp : StatusPageEvent()
}

sealed class StatusPageUiEvent {
    data class ShowSnackBar(val message: String) : StatusPageUiEvent()
}