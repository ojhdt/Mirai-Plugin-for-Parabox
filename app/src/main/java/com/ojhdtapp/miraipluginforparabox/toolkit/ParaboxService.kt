package com.ojhdtapp.miraipluginforparabox.toolkit

import android.content.Intent
import android.os.*
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.ojhdtapp.messagedto.ReceiveMessageDto
import com.ojhdtapp.messagedto.SendMessageDto
import com.ojhdtapp.miraipluginforparabox.core.util.CompletableDeferredWithTag
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@AndroidEntryPoint
abstract class ParaboxService : LifecycleService() {
    var serviceState = ParaboxKey.STATE_STOP
    lateinit var paraboxMessenger: Messenger
    private var clientMessenger: Messenger? = null
    private var mainAppMessenger: Messenger? = null

    val deferredMap = mutableMapOf<Long, CompletableDeferred<ParaboxResult>>()
    val messageUnreceivedMap = mutableMapOf<Long, ReceiveMessageDto>()

    abstract fun onStartParabox()
    abstract fun onStopParabox()
    abstract fun onStateUpdate(state: Int, message: String? = null)
    abstract fun customHandleMessage(msg: Message, metadata: ParaboxMetadata)
    abstract suspend fun onSendMessage(dto: SendMessageDto): Boolean
    abstract suspend fun onRecallMessage(messageId: Long): Boolean
    abstract fun onRefresh()
    fun startParabox(metadata: ParaboxMetadata) {
        if (serviceState in listOf<Int>(ParaboxKey.STATE_STOP, ParaboxKey.STATE_ERROR)) {
            onStartParabox()
            sendCommandResponse(
                isSuccess = true,
                metadata = metadata
            )
        } else {
            sendCommandResponse(
                isSuccess = false,
                metadata = metadata,
                errorCode = ParaboxKey.ERROR_REPEATED_CALL
            )
        }
    }

    fun stopParabox(metadata: ParaboxMetadata) {
        if (serviceState in listOf<Int>(ParaboxKey.STATE_RUNNING)) {
            onStopParabox()
            sendCommandResponse(
                isSuccess = true,
                metadata = metadata
            )
        } else {
            sendCommandResponse(
                isSuccess = false,
                metadata = metadata,
                errorCode = ParaboxKey.ERROR_REPEATED_CALL
            )
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
            sendCommandResponse(isSuccess = true, metadata = metadata)
        } else {
            sendCommandResponse(
                isSuccess = false,
                metadata = metadata,
                errorCode = ParaboxKey.ERROR_REPEATED_CALL
            )
        }
    }

    fun updateServiceState(state: Int, message: String? = null) {
        serviceState = state
        onStateUpdate(state, message)
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

    fun receiveMessage(dto: ReceiveMessageDto) {
        sendRequest(request = ParaboxKey.REQUEST_RECEIVE_MESSAGE,
            client = ParaboxKey.CLIENT_MAIN_APP,
            extra = Bundle().apply {
                putParcelable("dto", dto)
            },
            timeoutMillis = 6000,
            onResult = {
                if (it is ParaboxResult.Fail) {
                    messageUnreceivedMap[dto.messageId!!] = dto
                } else {
                    messageUnreceivedMap.remove(dto.messageId!!)
                }
            })
    }

    private fun sendMessage(metadata: ParaboxMetadata, dto: SendMessageDto) {
        lifecycleScope.launch {
            if (serviceState == ParaboxKey.STATE_RUNNING) {
                if (onSendMessage(dto)) {
                    // Success
                    sendCommandResponse(
                        isSuccess = true,
                        metadata = metadata
                    )
                } else {
                    sendCommandResponse(
                        isSuccess = false,
                        metadata = metadata,
                        errorCode = ParaboxKey.ERROR_SEND_FAILED
                    )
                }
            } else {
                sendCommandResponse(
                    isSuccess = false,
                    metadata = metadata,
                    errorCode = ParaboxKey.ERROR_DISCONNECTED
                )
            }
        }
    }

    private fun recallMessage(metadata: ParaboxMetadata, messageId: Long) {
        lifecycleScope.launch {
            if (serviceState == ParaboxKey.STATE_RUNNING) {
                if (onRecallMessage(messageId)) {
                    // Success
                    sendCommandResponse(
                        isSuccess = true,
                        metadata = metadata
                    )
                } else {
                    sendCommandResponse(
                        isSuccess = false,
                        metadata = metadata,
                        errorCode = ParaboxKey.ERROR_SEND_FAILED
                    )
                }
            } else {
                sendCommandResponse(
                    isSuccess = false,
                    metadata = metadata,
                    errorCode = ParaboxKey.ERROR_DISCONNECTED
                )
            }
        }
    }

    private fun getUnreceivedMessage(metadata: ParaboxMetadata) {
        messageUnreceivedMap.forEach {
            receiveMessage(it.value)
        }
        sendCommandResponse(
            true,
            metadata
        )
        onRefresh()
    }

    fun sendCommandResponse(
        isSuccess: Boolean,
        metadata: ParaboxMetadata,
        extra: Bundle = Bundle(),
        errorCode: Int? = null
    ) {
        Log.d("parabox", "before try sending result")
        if (isSuccess) {
            ParaboxResult.Success(
                command = metadata.commandOrRequest,
                timestamp = metadata.timestamp,
                obj = extra,
            )
        } else {
            ParaboxResult.Fail(
                command = metadata.commandOrRequest,
                timestamp = metadata.timestamp,
                errorCode = errorCode!!
            )
        }.also {
            Log.d("parabox", "try sending result")
            deferredMap[metadata.timestamp]?.complete(it)
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
                Log.d("parabox", "send back to activity")
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
                    val deferred = CompletableDeferred<ParaboxResult>()
                    deferredMap[timestamp] = deferred
                    coreSendRequest(timestamp, request, client, extra)
                    deferred.await().also {
                        Log.d("parabox", "deferred complete successfully")
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
            Log.d("parabox", "request sent")
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
            // 客户端类型判断
            when (msg.arg1) {
                ParaboxKey.CLIENT_CONTROLLER -> {
                    clientMessenger = msg.replyTo
                }

                ParaboxKey.CLIENT_MAIN_APP -> {
                    mainAppMessenger = msg.replyTo
                }
            }

            val obj = msg.obj as Bundle
            val metadata = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                obj.getParcelable("metadata", ParaboxMetadata::class.java)!!
            } else {
                obj.getParcelable<ParaboxMetadata>("metadata")!!
            }
            // 对command添加deferred
            when (msg.arg2) {
                ParaboxKey.TYPE_COMMAND -> {
                    lifecycleScope.launch {
                        try {
                            val deferred =
                                CompletableDeferred<ParaboxResult>()
                            deferredMap[metadata.timestamp] = deferred

                            // 指令种类判断
                            when (msg.what) {
                                ParaboxKey.COMMAND_START_SERVICE -> {
                                    Log.d("parabox", "command receiced")
                                    startParabox(metadata)
                                }

                                ParaboxKey.COMMAND_STOP_SERVICE -> {
                                    stopParabox(metadata)
                                }

                                ParaboxKey.COMMAND_FORCE_STOP_SERVICE -> {
                                    forceStopParabox(metadata)
                                }

                                ParaboxKey.COMMAND_SEND_MESSAGE -> {
                                    val dto = obj.getParcelable<SendMessageDto>("dto")
                                    if (dto == null) {
                                        sendCommandResponse(
                                            isSuccess = false,
                                            metadata = metadata,
                                            errorCode = ParaboxKey.ERROR_RESOURCE_NOT_FOUND
                                        )
                                    } else {
                                        sendMessage(metadata, dto)
                                    }
                                }

                                ParaboxKey.COMMAND_RECALL_MESSAGE -> {
                                    val messageId = obj.getLong("messageId")
                                    if (messageId == null) {
                                        sendCommandResponse(
                                            isSuccess = false,
                                            metadata = metadata,
                                            errorCode = ParaboxKey.ERROR_RESOURCE_NOT_FOUND
                                        )
                                    } else {
                                        recallMessage(metadata, messageId)
                                    }
                                }

                                ParaboxKey.COMMAND_GET_UNRECEIVED_MESSAGE -> {
                                    getUnreceivedMessage(metadata)
                                }

                                else -> customHandleMessage(msg, metadata)
                            }
                            deferred.await().also {
                                Log.d("parabox", "first deferred complete")
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
                        Log.d("parabox", "tr complete second deferred")
                        deferredMap[sendTimestamp]?.complete(it)
                    }
                }
            }
        }
    }
}


