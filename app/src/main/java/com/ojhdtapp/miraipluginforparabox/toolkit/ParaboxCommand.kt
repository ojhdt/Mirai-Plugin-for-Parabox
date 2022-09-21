package com.ojhdtapp.miraipluginforparabox.toolkit

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

object ParaboxKey {
    const val TYPE_MAIN_APP = 0
    const val TYPE_CLIENT = 1

    const val COMMAND_START_SERVICE = 10
    const val COMMAND_STOP_SERVICE = 11

    const val ERROR_TIMEOUT = 20
    const val ERROR_DISCONNECTED = 21

    const val STATE_STOP = 30
    const val STATE_PAUSE = 31
    const val STATE_ERROR = 32
    const val STATE_LOADING = 33
    const val STATE_RUNNING = 34
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