package com.ojhdtapp.miraipluginforparabox.toolkit

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ojhdtapp.miraipluginforparabox.core.util.CompletableDeferredWithTag
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

abstract class ParaboxActivity<T>(private val serviceClass: Class<T>) : AppCompatActivity() {
    abstract fun onParaboxServiceConnected()
    abstract fun onParaboxServiceDisconnected()
    abstract fun onParaboxServiceStateChanged(state: Int, message: String)
    abstract fun customHandleMessage(msg: Message, metadata: ParaboxMetadata)

    var paraboxService: Messenger? = null
    private lateinit var client: Messenger
    private lateinit var paraboxServiceConnection: ServiceConnection

    var deferredMap = mutableMapOf<Long, CompletableDeferredWithTag<Long, ParaboxResult>>()

    /*/
    推荐于onStart运行
     */
    fun bindParaboxService() {
        val intent = Intent(this, serviceClass)
        startService(intent)
        bindService(intent, paraboxServiceConnection, BIND_AUTO_CREATE)
    }

    /*/
    推荐于onStop执行
     */
    fun stopParaboxService() {
        if (paraboxService != null) {
            unbindService(paraboxServiceConnection)
        }
    }

    fun sendCommand(command: Int, extra: Bundle = Bundle(), timeoutMillis: Long = 3000, onResult: (ParaboxResult) -> Unit) {
        lifecycleScope.launch {
            val timestamp = System.currentTimeMillis()
            try {
                withTimeout(timeoutMillis) {
                    val deferred = CompletableDeferredWithTag<Long, ParaboxResult>(timestamp)
                    deferredMap[timestamp] = deferred
                    coreSendCommand(timestamp, command, extra)
                    deferred.await().also {
                        onResult(it)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                deferredMap[timestamp]?.cancel()
                onResult(
                    ParaboxResult.Fail(
                        command,
                        timestamp,
                        ParaboxKey.ERROR_TIMEOUT
                    )
                )
            } catch (e: RemoteException) {
                deferredMap[timestamp]?.cancel()
                onResult(
                    ParaboxResult.Fail(
                        command,
                        timestamp,
                        ParaboxKey.ERROR_DISCONNECTED
                    )
                )
            }
        }
    }

    /*/
    what: 指令
    arg1: 客户端类型
    arg2: 指令类型
    obj: Bundle
     */
    private fun coreSendCommand(timestamp: Long, command: Int, extra: Bundle = Bundle()) {
        if (paraboxService == null) {
            deferredMap[timestamp]?.complete(
                timestamp,
                ParaboxResult.Fail(
                    command, timestamp,
                    ParaboxKey.ERROR_DISCONNECTED
                )
            )
        } else {
            val msg = Message.obtain(null, command, ParaboxKey.CLIENT_CONTROLLER, ParaboxKey.TYPE_COMMAND, extra.apply {
                putParcelable("metadata", ParaboxMetadata(
                    commandOrRequest = command,
                    timestamp = timestamp,
                    sender = ParaboxKey.CLIENT_CONTROLLER
                ))
            }).apply {
                replyTo = client
            }
            paraboxService!!.send(msg)
        }
    }

    fun sendRequestResponse(
        isSuccess: Boolean,
        metadata: ParaboxMetadata,
        extra: Bundle = Bundle(),
        errorCode: Int? = null
    ) {
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
            deferredMap[metadata.timestamp]?.complete(metadata.timestamp, it)
//            coreSendCommandResponse(isSuccess, metadata, it)
        }
    }

    private fun coreSendRequestResponse(
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
                    ParaboxKey.TYPE_REQUEST,
                    extra.apply {
                        putBoolean("isSuccess", isSuccess)
                        putParcelable("metadata", metadata)
                        putParcelable("result", result)
                    }).apply {
                    replyTo = client
                }
                paraboxService?.send(msg)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        client = Messenger(ParaboxServiceHandler())
        paraboxServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
                paraboxService = Messenger(p1)
                onParaboxServiceConnected()
            }

            override fun onServiceDisconnected(p0: ComponentName?) {
                paraboxService = null
                onParaboxServiceDisconnected()
            }

        }
    }

    inner class ParaboxServiceHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val obj = msg.obj as Bundle
            when(msg.arg2){
                ParaboxKey.TYPE_REQUEST -> {
                    val metadata = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        obj.getParcelable("metadata", ParaboxMetadata::class.java)!!
                    } else {
                        obj.getParcelable<ParaboxMetadata>("metadata")!!
                    }
                    lifecycleScope.launch {
                        try {
                            val deferred =
                                CompletableDeferredWithTag<Long, ParaboxResult>(metadata.timestamp)
                            deferredMap[metadata.timestamp] = deferred

                            // 指令种类判断
                            when (msg.what) {
                                else -> customHandleMessage(msg, metadata)
                            }

                            deferred.await().also {
                                val resObj = if (it is ParaboxResult.Success) {
                                    it.obj
                                } else Bundle()
                                coreSendRequestResponse(
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
                ParaboxKey.TYPE_COMMAND -> {
                    val metadata = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        obj.getParcelable("metadata", ParaboxMetadata::class.java)!!
                    } else {
                        obj.getParcelable<ParaboxMetadata>("metadata")!!
                    }
                    val sendTimestamp = metadata.timestamp
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
                ParaboxKey.TYPE_NOTIFICATION -> {
                    when(msg.what){
                        ParaboxKey.NOTIFICATION_STATE_UPDATE -> {
                            val state = obj.getInt("state", ParaboxKey.STATE_ERROR)
                            val message = obj.getString("message", "")
                            onParaboxServiceStateChanged(state, message)
                        }
                    }
                }
            }
            super.handleMessage(msg)
        }
    }
}