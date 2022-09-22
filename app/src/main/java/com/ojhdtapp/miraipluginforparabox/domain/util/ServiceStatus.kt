package com.ojhdtapp.miraipluginforparabox.domain.util

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class ServiceStatus(open val message: String) : Parcelable{
    @Parcelize data class Loading(override val message: String): ServiceStatus(message)
    @Parcelize data class Pause(override val message: String): ServiceStatus(message)
    @Parcelize data class Error(override val message: String) : ServiceStatus(message)
    @Parcelize data class Running(override val message: String) : ServiceStatus(message)
    @Parcelize object Stop : ServiceStatus("")
}
