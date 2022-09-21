package com.ojhdtapp.miraipluginforparabox.toolkit

import android.app.Service
import android.content.Intent
import android.os.*
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleService

abstract class ParaboxService : LifecycleService() {
    var serviceState = ParaboxKey.STATE_STOP
    lateinit var paraboxMessenger: Messenger
    private var clientMessenger: Messenger? = null
    private var mainAppMessenger: Messenger? = null

    abstract fun onStartParabox()
    abstract fun onStopParabox()
    abstract fun onStateUpdate()
    abstract fun customHandleMessage(msg: Message, metadata: ParaboxMetadata)
    fun startParabox(metadata: ParaboxMetadata) {
        if (serviceState in listOf<Int>(ParaboxKey.STATE_STOP, ParaboxKey.STATE_ERROR)) {
            onStartParabox()
            sendResponse(true, metadata)
        } else {
            sendResponse(false, metadata, ParaboxKey.ERROR_REPEATED_CALL)
        }
    }

    fun stopParabox(metadata: ParaboxMetadata) {
        if (serviceState in listOf<Int>(ParaboxKey.STATE_RUNNING)) {
            onStopParabox()
            sendResponse(true, metadata)
        } else {
            sendResponse(false, metadata, ParaboxKey.ERROR_REPEATED_CALL)
        }
    }

    fun forceStopParabox(metadata: ParaboxMetadata) {
        if (serviceState in listOf<Int>(
                ParaboxKey.STATE_RUNNING,
                ParaboxKey.STATE_ERROR,
                ParaboxKey.STATE_LOADING,
                ParaboxKey.STATE_PAUSE
            )
        ) {
            onStopParabox()
            sendResponse(true, metadata)
        } else {
            sendResponse(false, metadata, ParaboxKey.ERROR_REPEATED_CALL)
        }
    }

    private fun updateServiceState(state: Int) {
        serviceState = state
        onStateUpdate()
        noticeStateUpdate()
    }

    fun noticeStateUpdate() {

    }

    fun receiveMessage() {

    }

    abstract fun onSendMessage()

    private fun sendResponse(
        isSuccess: Boolean,
        metadata: ParaboxMetadata,
        errorCode: Int? = null
    ) {
        if (isSuccess) {
            ParaboxCommandResult.Success(
                command = metadata.command,
                timestamp = metadata.timestamp,
            )
        } else {
            ParaboxCommandResult.Fail(
                command = metadata.command,
                timestamp = metadata.timestamp,
                errorCode = errorCode!!
            )
        }.also {
            coreSendResponse(isSuccess, metadata, it)
        }
    }

    private fun coreSendResponse(
        isSuccess: Boolean,
        metadata: ParaboxMetadata,
        result: ParaboxCommandResult,
        extra: Bundle = Bundle()
    ) {
        when (metadata.type) {
            ParaboxKey.TYPE_MAIN_APP -> {}
            ParaboxKey.TYPE_CLIENT -> {
                val msg = Message.obtain(null, metadata.command, ParaboxKey.TYPE_CLIENT, 0, extra.apply {
                    putBoolean("isSuccess", isSuccess)
                    putParcelable("metadata", metadata)
                    putParcelable("result", result)
                }).apply {
                    replyTo = paraboxMessenger
                }
                clientMessenger?.send(msg)
            }
        }
    }

    override fun onCreate() {
        paraboxMessenger = Messenger(CommandHandler())
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return paraboxMessenger.binder
    }


    inner class CommandHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val obj = msg.obj as Bundle
            val metadata = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                obj.getParcelable("metadata", ParaboxMetadata::class.java)
            } else {
                obj.getParcelable<ParaboxMetadata>("metadata")
            }
            when (msg.arg1) {
                ParaboxKey.TYPE_CLIENT -> {
                    clientMessenger = msg.replyTo
                    when (msg.what) {
                        ParaboxKey.COMMAND_START_SERVICE -> {
                            startParabox(metadata!!)
                        }
                        ParaboxKey.COMMAND_STOP_SERVICE -> {
                            stopParabox(metadata!!)
                        }
                        ParaboxKey.COMMAND_FORCE_STOP_SERVICE -> {
                            forceStopParabox(metadata!!)
                        }
                        else -> customHandleMessage(msg, metadata!!)
                    }
                }
                ParaboxKey.TYPE_MAIN_APP -> {
                    mainAppMessenger = msg.replyTo
                }
            }
        }
    }
}


