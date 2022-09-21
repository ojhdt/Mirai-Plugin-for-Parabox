package com.ojhdtapp.miraipluginforparabox.toolkit

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

object ParaboxKey {
    const val COMMAND_START_SERVICE = 10
    const val COMMAND_STOP_SERVICE = 11

    const val RESPONSE_TIMEOUT = 20
    const val RESPONSE_DISCONNECTED = 21
}

@Parcelize
sealed class ParaboxCommandResult(
    open val command: Int,
    open val timestamp: Long,
) : Parcelable {
    data class Success(
        override val command: Int,
        override val timestamp: Long,
    ) : ParaboxCommandResult(command = command, timestamp = timestamp)

    data class Fail(
        override val command: Int,
        override val timestamp: Long,
        val responseKey: Int,
    ) : ParaboxCommandResult(command = command, timestamp = timestamp)
}