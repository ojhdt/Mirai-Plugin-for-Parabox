package com.ojhdtapp.miraipluginforparabox.toolkit

import android.content.Intent
import android.os.*
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.ojhdtapp.miraipluginforparabox.core.util.CompletableDeferredWithTag
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

abstract class ParaboxService : LifecycleService() {
    var serviceState = ParaboxKey.STATE_STOP
    lateinit var paraboxMessenger: Messenger
    private var clientMessenger: Messenger? = null
    private var mainAppMessenger: Messenger? = null

    var deferredMap = mutableMapOf<Long, CompletableDeferredWithTag<Long, ParaboxResult>>()

    abstract fun onStartParabox()
    abstract fun onStopParabox()
    abstract fun onStateUpdate()
    abstract fun customHandleMessage(msg: Message, metadata: ParaboxMetadata)
    fun startParabox(metadata: ParaboxMetadata) {
        if (serviceState in listOf<Int>(ParaboxKey.STATE_STOP, ParaboxKey.STATE_ERROR)) {
            onStartParabox()
            sendCommandResponse(true, metadata)
        } else {
            sendCommandResponse(false, metadata, ParaboxKey.ERROR_REPEATED_CALL)
        }
    }

    fun stopParabox(metadata: ParaboxMetadata) {
        if (serviceState in listOf<Int>(ParaboxKey.STATE_RUNNING)) {
            onStopParabox()
            sendCommandResponse(true, metadata)
        } else {
            sendCommandResponse(false, metadata, ParaboxKey.ERROR_REPEATED_CALL)
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
            sendCommandResponse(true, metadata)
        } else {
            sendCommandResponse(false, metadata, ParaboxKey.ERROR_REPEATED_CALL)
        }
    }

    fun updateServiceState(state: Int, message: String? = null) {
        serviceState = state
        onStateUpdate()
        sendNotification(ParaboxKey.NOTIFICATION_STATE_UPDATE, Bundle().apply {
            putInt("state", state)
            message?.let { putString("message", it) }
        })
    }

    fun sendNotification(notification: Int, extra: Bundle = Bundle()) {
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

    private fun sendCommandResponse(
        isSuccess: Boolean,
        metadata: ParaboxMetadata,
        errorCode: Int? = null
    ) {
        if (isSuccess) {
            ParaboxResult.Success(
                command = metadata.commandOrRequest,
                timestamp = metadata.timestamp,
            )
        } else {
            ParaboxResult.Fail(
                command = metadata.commandOrRequest,
                timestamp = metadata.timestamp,
                errorCode = errorCode!!
            )
        }.also {
            deferredMap[metadata.timestamp]?.complete(metadata.timestamp, it)
//            coreSendCommandResponse(isSuccess, metadata, it)
        }
    }

    private fun coreSendCommandResponse(
        isSuccess: Boolean,
        metadata: ParaboxMetadata,
        result: ParaboxResult,
        extra: Bundle = Bundle()
    ) {
        when (metadata.sender) {
            ParaboxKey.CLIENT_MAIN_APP -> {}
            ParaboxKey.CLIENT_CONTROLLER -> {
                val msg = Message.obtain(
                    null,
                    metadata.commandOrRequest,
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

    fun sendRequest(
        request: Int,
        client: Int,
        extra: Bundle = Bundle(),
        timeoutMillis: Long = 3000,
        onResult: (ParaboxResult) -> Unit
    ) {
        lifecycleScope.launch {
            val timestamp = System.currentTimeMillis()
            try {
                withTimeout(timeoutMillis) {
                    val deferred = CompletableDeferredWithTag<Long, ParaboxResult>(timestamp)
                    deferredMap[timestamp] = deferred
                    coreSendRequest(timestamp, request, client, extra)
                    deferred.await().also {
                        onResult(it)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                deferredMap[timestamp]?.cancel()
                onResult(
                    ParaboxResult.Fail(
                        request,
                        timestamp,
                        ParaboxKey.ERROR_TIMEOUT
                    )
                )
            } catch (e: RemoteException) {
                deferredMap[timestamp]?.cancel()
                onResult(
                    ParaboxResult.Fail(
                        request,
                        timestamp,
                        ParaboxKey.ERROR_DISCONNECTED
                    )
                )
            }
        }
    }

    private fun coreSendRequest(
        timestamp: Long,
        request: Int,
        client: Int,
        extra: Bundle = Bundle()
    ) {
        val targetClient = when (client) {
            ParaboxKey.CLIENT_CONTROLLER -> clientMessenger
            ParaboxKey.CLIENT_MAIN_APP -> mainAppMessenger
            else -> null
        }
        if (targetClient == null) {
            deferredMap[timestamp]?.complete(
                timestamp,
                ParaboxResult.Fail(
                    request, timestamp,
                    ParaboxKey.ERROR_DISCONNECTED
                )
            )
        } else {
            val msg = Message.obtain(null, request, client, ParaboxKey.TYPE_REQUEST, extra.apply {
                putParcelable(
                    "metadata", ParaboxMetadata(
                        commandOrRequest = request,
                        timestamp = timestamp,
                        sender = ParaboxKey.CLIENT_SERVICE
                    )
                )
            }).apply {
                replyTo = paraboxMessenger
            }
            targetClient.send(msg)
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
                obj.getParcelable("metadata", ParaboxMetadata::class.java)!!
            } else {
                obj.getParcelable<ParaboxMetadata>("metadata")!!
            }
            // 对command添加deferred
            when(msg.arg2){
                ParaboxKey.TYPE_COMMAND -> {
                    lifecycleScope.launch {
                        try {
                            val deferred =
                                CompletableDeferredWithTag<Long, ParaboxResult>(metadata.timestamp)
                            deferredMap[metadata.timestamp] = deferred

                            // 指令种类判断
                            when (msg.what) {
                                ParaboxKey.COMMAND_START_SERVICE -> {
                                    startParabox(metadata)
                                }
                                ParaboxKey.COMMAND_STOP_SERVICE -> {
                                    stopParabox(metadata)
                                }
                                ParaboxKey.COMMAND_FORCE_STOP_SERVICE -> {
                                    forceStopParabox(metadata)
                                }
                                else -> customHandleMessage(msg, metadata)
                            }
                            deferred.await().also {
                                val resObj = if (it is ParaboxResult.Success) {
                                    it.obj
                                } else Bundle()
                                coreSendCommandResponse(
                                    it is ParaboxResult.Success,
                                    metadata,
                                    it,
                                    resObj
                                )
                            }
                        } catch (e: RemoteException) {
                            e.printStackTrace()
                        }
                    }
                }
                ParaboxKey.TYPE_REQUEST -> {
                    val sendTimestamp = obj.getParcelable<ParaboxMetadata>("metadata")!!.timestamp
                    val isSuccess = obj.getBoolean("isSuccess")
                    val result = if (isSuccess) {
                        obj.getParcelable<ParaboxResult.Success>("result")
                    } else {
                        obj.getParcelable<ParaboxResult.Fail>("result")
                    }
                    result?.let {
                        deferredMap[sendTimestamp]?.complete(sendTimestamp, it)
                    }
                }
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
        }
    }
}


