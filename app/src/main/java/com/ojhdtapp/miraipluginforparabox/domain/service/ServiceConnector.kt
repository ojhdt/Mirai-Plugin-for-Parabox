package com.ojhdtapp.miraipluginforparabox.domain.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import androidx.lifecycle.ViewModel
import com.ojhdtapp.miraipluginforparabox.domain.util.LoginResource
import com.ojhdtapp.miraipluginforparabox.domain.util.LoginResourceType
import com.ojhdtapp.miraipluginforparabox.domain.util.ServiceStatus
import com.ojhdtapp.miraipluginforparabox.ui.status.StatusPageViewModel

class ServiceConnector(private val context: Context, private val vm: StatusPageViewModel) :
    ConnCommandInterface {
    private var sMessenger: Messenger? = null
    private val interfaceMessenger = Messenger(ConnHandler())
    var isConnected = false
        private set

    fun initializeAllState() {
        vm.updateLoginResourceStateFlow(LoginResource.None)
        vm.updateServiceStatusStateFlow(ServiceStatus.Stop)
    }

    fun startAndBind() {
        val intent = Intent(context, ConnService::class.java)
        context.startService(intent)
        context.bindService(intent, Connection(), Context.BIND_AUTO_CREATE)
    }

    override fun miraiStart() {
        sMessenger?.send(Message.obtain(null, ConnKey.MSG_COMMAND, Bundle().apply {
            putInt("command", ConnKey.MSG_COMMAND_START_SERVICE)
        }).apply {
            replyTo = interfaceMessenger
        })
    }

    override fun miraiStop() {
        sMessenger?.send(Message.obtain(null, ConnKey.MSG_COMMAND, Bundle().apply {
            putInt("command", ConnKey.MSG_COMMAND_STOP_SERVICE)
        }).apply {
            replyTo = interfaceMessenger
        })
    }

    override fun miraiLogin() {
        sMessenger?.send(Message.obtain(null, ConnKey.MSG_COMMAND, Bundle().apply {
            putInt("command", ConnKey.MSG_COMMAND_LOGIN)
        }).apply {
            replyTo = interfaceMessenger
        })
    }

    override fun onLoginStateChanged(resource: LoginResource) {
        vm.updateLoginResourceStateFlow(resource)
    }

    override fun submitVerificationResult(result: String) {
        if (isConnected) {
            sMessenger?.send(Message.obtain(null, ConnKey.MSG_COMMAND, Bundle().apply {
                putInt("command", ConnKey.MSG_COMMAND_SUBMIT_VERIFICATION_RESULT)
                putString("value", result)
            }).apply {
                replyTo = interfaceMessenger
            })
        }
    }

    inner class Connection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            isConnected = true
            sMessenger = Messenger(service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isConnected = false
            sMessenger = null
            initializeAllState()
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
                else -> super.handleMessage(msg)
            }
        }
    }
}
