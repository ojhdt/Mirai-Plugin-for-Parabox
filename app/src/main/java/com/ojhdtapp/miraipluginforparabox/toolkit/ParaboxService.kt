package com.ojhdtapp.miraipluginforparabox.toolkit

import android.content.Intent
import android.os.*
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

    private fun updateServiceState(state: Int, message: String? = null) {
        serviceState = state
        onStateUpdate()
        sendNotification(ParaboxKey.NOTIFICATION_STATE_UPDATE, Bundle().apply {
            putInt("state", state)
            message?.let { putString("message", it) }
        })
    }

    fun sendNotification(notification: Int ,extra: Bundle = Bundle()) {
        val timestamp = System.currentTimeMillis()
        val msg = Message.obtain(
            null,
            notification,
            0,
            ParaboxKey.TYPE_NOTIFICATION,
            extra.apply {
                putLong("timestamp", timestamp)
            }).apply {
            replyTo = paraboxMessenger
        }
        clientMessenger?.send(msg)
        mainAppMessenger?.send(msg)
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
        when (metadata.client) {
            ParaboxKey.CLIENT_MAIN_APP -> {}
            ParaboxKey.CLIENT_CONTROLLER -> {
                val msg = Message.obtain(
                    null,
                    metadata.command,
                    ParaboxKey.CLIENT_CONTROLLER,
                    ParaboxKey.TYPE_COMMAND,
                    extra.apply {
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
            // 客户端类型判断
            when (msg.arg1) {
                ParaboxKey.CLIENT_CONTROLLER -> {
                    clientMessenger = msg.replyTo
                }
                ParaboxKey.CLIENT_MAIN_APP -> {
                    mainAppMessenger = msg.replyTo
                }
            }
            // 指令种类判断
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
    }
}


