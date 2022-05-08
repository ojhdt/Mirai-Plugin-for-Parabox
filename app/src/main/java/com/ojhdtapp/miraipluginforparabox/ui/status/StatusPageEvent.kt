package com.ojhdtapp.miraipluginforparabox.ui.status

sealed class StatusPageEvent {
    data class OnLoginClick(val accountNum: Long, val passwd:String) : StatusPageEvent()
}