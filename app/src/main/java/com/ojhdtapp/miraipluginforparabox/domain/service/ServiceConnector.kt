package com.ojhdtapp.miraipluginforparabox.domain.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import androidx.lifecycle.ViewModel
import com.ojhdtapp.miraipluginforparabox.core.util.CompletableDeferredWithTag
import com.ojhdtapp.miraipluginforparabox.domain.util.LoginResource
import com.ojhdtapp.miraipluginforparabox.domain.util.LoginResourceType
import com.ojhdtapp.miraipluginforparabox.domain.util.ServiceStatus
import com.ojhdtapp.miraipluginforparabox.ui.status.StatusPageViewModel
import kotlinx.coroutines.CompletableDeferred

class ServiceConnector(private val context: Context, private val vm: StatusPageViewModel) :
    ConnCommandInterface {
    private var sMessenger: Messenger? = null
    private val interfaceMessenger = Messenger(ConnHandler())
    private var connectionDeferred : CompletableDeferred<ServiceStatus>? = null
    private var listeningDeferred: CompletableDeferredWithTag<Long, ServiceStatus>? = null
    private var connection : Connection? = null
    private var isConnected = false

    fun initializeAllState() {
//        vm.updateLoginResourceStateFlow(LoginResource.None)
//        vm.updateServiceStatusStateFlow(ServiceStatus.Stop)
        connectionDeferred = null
        listeningDeferred = null
        Log.d("parabox", "initialize all state")
    }

    suspend fun startAndBind() : ServiceStatus{
        val timestamp = System.currentTimeMillis()
        val intent = Intent(context, ConnService::class.java)
        context.startService(intent)
        connection = Connection()
        context.bindService(intent, connection!!, Context.BIND_AUTO_CREATE)
        connectionDeferred = CompletableDeferred()
        return connectionDeferred!!.await()
    }

    override suspend fun miraiStart(): ServiceStatus =
        if (isConnected) {
            val timestamp = System.currentTimeMillis()
            sMessenger?.send(Message.obtain(null, ConnKey.MSG_COMMAND, Bundle().apply {
                putInt("command", ConnKey.MSG_COMMAND_START_SERVICE)
                putLong("timestamp", timestamp)
            }).apply {
                replyTo = interfaceMessenger
            })
            listeningDeferred = CompletableDeferredWithTag(timestamp)
            listeningDeferred!!.await()
        } else {
            ServiceStatus.Error("???????????????????????????")
        }


    override suspend fun miraiStop(): ServiceStatus {
        val timestamp = System.currentTimeMillis()
        sMessenger?.send(Message.obtain(null, ConnKey.MSG_COMMAND, Bundle().apply {
            putInt("command", ConnKey.MSG_COMMAND_STOP_SERVICE)
            putLong("timestamp", timestamp)
        }).apply {
            replyTo = interfaceMessenger
        })
        listeningDeferred = CompletableDeferredWithTag(timestamp)
        return listeningDeferred!!.await().also {
            connection?.let { conn -> context.unbindService(conn) }
        }
    }

    override suspend fun miraiLogin(): ServiceStatus =
        if (isConnected) {
            val timestamp = System.currentTimeMillis()
            sMessenger?.send(Message.obtain(null, ConnKey.MSG_COMMAND, Bundle().apply {
                putInt("command", ConnKey.MSG_COMMAND_LOGIN)
                putLong("timestamp", timestamp)
            }).apply {
                replyTo = interfaceMessenger
            })
            listeningDeferred = CompletableDeferredWithTag(timestamp)
            listeningDeferred!!.await()
        } else {
            ServiceStatus.Error("???????????????????????????")
        }

    override fun onLoginStateChanged(resource: LoginResource) {
        vm.updateLoginResourceStateFlow(resource)
    }

    override suspend fun submitVerificationResult(result: String): ServiceStatus =
        if (isConnected) {
            val timestamp = System.currentTimeMillis()
            sMessenger?.send(Message.obtain(null, ConnKey.MSG_COMMAND, Bundle().apply {
                putInt("command", ConnKey.MSG_COMMAND_SUBMIT_VERIFICATION_RESULT)
                putLong("timestamp", timestamp)
                putString("value", result)
            }).apply {
                replyTo = interfaceMessenger
            })
            listeningDeferred = CompletableDeferredWithTag(timestamp)
            listeningDeferred!!.await()
        } else {
            ServiceStatus.Error("???????????????????????????")
        }

    fun miraiForceStop(){
        sMessenger?.send(Message.obtain(null, ConnKey.MSG_COMMAND, Bundle().apply {
            putInt("command", ConnKey.MSG_COMMAND_STOP_SERVICE)
        }).apply {
            replyTo = interfaceMessenger
        })
        connection?.let { context.unbindService(it) }
    }


    inner class Connection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d("parabox", "service connected")
            isConnected = true
            sMessenger = Messenger(service)
            connectionDeferred?.complete(ServiceStatus.Loading("?????????????????????"))
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d("parabox", "service disconnected")
            isConnected = false
            sMessenger = null
            initializeAllState()
            connectionDeferred?.complete(ServiceStatus.Error("???????????????????????????"))
        }

    }

    inner class ConnHandler : Handler() {
        override fun handleMessage(msg: Message) {
//            super.handleMessage(msg)
//            val receivedMsg = (msg.obj as Bundle).getString("str") ?: "message lost"
//            Log.d("parabox", "message back from client: $receivedMsg")
            when (msg.what) {
                ConnKey.MSG_COMMAND -> {
                    when ((msg.obj as Bundle).getInt("command", 10)) {
                        ConnKey.MSG_COMMAND_ON_LOGIN_STATE_CHANGED -> {
                            val value: LoginResource =
                                when ((msg.obj as Bundle).getInt("type", LoginResourceType.None)) {
                                    LoginResourceType.PicCaptcha -> (msg.obj as Bundle).getParcelable<LoginResource.PicCaptcha>(
                                        "value"
                                    ) ?: LoginResource.None
                                    LoginResourceType.SliderCaptcha -> (msg.obj as Bundle).getParcelable<LoginResource.SliderCaptcha>(
                                        "value"
                                    ) ?: LoginResource.None
                                    LoginResourceType.UnsafeDeviceLoginVerify -> (msg.obj as Bundle).getParcelable<LoginResource.UnsafeDeviceLoginVerify>(
                                        "value"
                                    ) ?: LoginResource.None
                                    else -> LoginResource.None
                                }
                            onLoginStateChanged(value)
                        }
                        else -> {}
                    }
                }
                ConnKey.MSG_RESPONSE -> {
                    val status = (msg.obj as Bundle).getInt("status", ConnKey.FAILURE)
                    when ((msg.obj as Bundle).getInt("command", -1)) {
                        ConnKey.MSG_RESPONSE_START_SERVICE -> {
                            listeningDeferred?.complete(
                                (msg.obj as Bundle).getLong("timestamp", -1L),
                                (msg.obj as Bundle).getParcelable<ServiceStatus>("value")
                                    ?: ServiceStatus.Error("????????????????????????????????????")
                            )
                        }
                        ConnKey.MSG_RESPONSE_STOP_SERVICE -> {
                            listeningDeferred?.complete(
                                (msg.obj as Bundle).getLong("timestamp", -1L),
                                (msg.obj as Bundle).getParcelable<ServiceStatus>("value")
                                    ?: ServiceStatus.Error("????????????????????????????????????")
                            )
                        }
                        ConnKey.MSG_RESPONSE_LOGIN -> {
                            listeningDeferred?.complete(
                                (msg.obj as Bundle).getLong("timestamp", -1L),
                                (msg.obj as Bundle).getParcelable<ServiceStatus>("value")
                                    ?: ServiceStatus.Error("????????????????????????????????????")
                            )
                        }
                        ConnKey.MSG_RESPONSE_SUBMIT_VERIFICATION_RESULT -> {
                            (msg.obj as Bundle).getString("value")?.let {
                            }
                        }
                        else -> {}
                    }
                }
                else -> super.handleMessage(msg)
            }
        }
    }
}
