package com.ojhdtapp.miraipluginforparabox.domain.service

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.*
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.ojhdtapp.miraipluginforparabox.domain.model.Secret
import com.ojhdtapp.miraipluginforparabox.domain.repository.MainRepository
import com.ojhdtapp.miraipluginforparabox.domain.service.ConnKey
import com.ojhdtapp.miraipluginforparabox.domain.util.LoginResource
import com.ojhdtapp.miraipluginforparabox.domain.util.LoginResourceType
import com.ojhdtapp.miraipluginforparabox.domain.util.ServiceStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import net.mamoe.mirai.Bot
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.Listener
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.network.LoginFailedException
import net.mamoe.mirai.utils.LoginSolver
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine
import kotlin.system.exitProcess

@AndroidEntryPoint
class ConnService @Inject constructor(
    private val repository: MainRepository
) : ConnCommandInterface, LifecycleService() {

    private lateinit var bot: Bot
    private var listener: Listener<FriendMessageEvent>? = null
    private lateinit var mLoginSolver: AndroidLoginSolver
    private lateinit var sMessenger: Messenger
    private var cMessenger: Messenger? = null
    private var interfaceMessenger: Messenger? = null

    private var isRunning = false

    private suspend fun miraiMain(accountNum: Long, passwd: String) {
        bot = BotFactory.newBot(accountNum, passwd) {
            loginSolver = mLoginSolver
            cacheDir = getExternalFilesDir("cache")!!.absoluteFile
        }
        try {
            bot.login()
            registerMessageReceiver()
            interfaceMessenger?.send(
                Message.obtain(null, ConnKey.MSG_RESPONSE, Bundle().apply {
                    putInt("command", ConnKey.MSG_RESPONSE_LOGIN)
                    putInt("status", ConnKey.SUCCESS)
                    putParcelable("value", ServiceStatus.Running("Mirai Core - "))
                })
            )
        } catch (e: LoginFailedException) {
            interfaceMessenger?.send(
                Message.obtain(null, ConnKey.MSG_RESPONSE, Bundle().apply {
                    putInt("command", ConnKey.MSG_RESPONSE_LOGIN)
                    putInt("status", ConnKey.FAILURE)
                    putParcelable("value", ServiceStatus.Error("登陆失败，请检查账户信息和网络连接"))
                })
            )
        }

    }

    private fun registerMessageReceiver() {
        listener =
            bot.eventChannel.subscribeAlways<net.mamoe.mirai.event.events.FriendMessageEvent> { event ->
                Log.d("aaa", "${event.senderName}:${event.message}")
                event.subject.sendMessage("Hello from mirai!")
            }
    }

    private fun unRegisterMessageReceiver() {
        if (listener?.isActive == true) {
            listener!!.complete()
            listener = null
        }
    }

    override fun onCreate() {
        super.onCreate()
        mLoginSolver = AndroidLoginSolver()
        sMessenger = Messenger(ConnHandler())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("parabox", "service started")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return sMessenger.binder
    }

    fun send(str: String) {
        if (cMessenger == null) {
            throw RemoteException("not connected")
        }
        try {
            // Message.obtain
            cMessenger!!.send(Message().apply {
                obj = Bundle().apply {
                    putString("str", str)
                }
            })
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    inner class ConnHandler : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                ConnKey.MSG_MESSAGE -> {
                    cMessenger = msg.replyTo
                    val str = (msg.obj as Bundle).getString("str") ?: "error"
                    Log.d("parabox", "message from cliect: $str")
                }
                ConnKey.MSG_COMMAND -> {
                    interfaceMessenger = msg.replyTo
                    when ((msg.obj as Bundle).getInt("command", 10)) {
                        ConnKey.MSG_COMMAND_START_SERVICE -> {
                            miraiStart()
                        }
                        ConnKey.MSG_COMMAND_STOP_SERVICE -> {
                            miraiStop()
                        }
                        ConnKey.MSG_COMMAND_LOGIN -> {
                            miraiLogin()
                        }
                        ConnKey.MSG_COMMAND_SUBMIT_VERIFICATION_RESULT -> {
                            (msg.obj as Bundle).getString("value")?.let {
                                mLoginSolver.submitVerificationResult(it)
                            }
                        }
                        else -> {}
                    }
                }
                else -> super.handleMessage(msg)
            }
            cMessenger = msg.replyTo

        }

    }

    inner class AndroidLoginSolver() : LoginSolver() {
        lateinit var verificationResult: CompletableDeferred<String>
        //    lateinit var captchaData: ByteArray
//    lateinit var url: String

        fun submitVerificationResult(result: String) {
            verificationResult.complete(result)
        }

        override suspend fun onSolvePicCaptcha(bot: Bot, data: ByteArray): String {
            Log.d("parabox", "onSolvePicCaptcha")
            verificationResult = CompletableDeferred()
//        captchaData = data
            val bm = BitmapFactory.decodeByteArray(data, 0, data.size)
            onLoginStateChanged(LoginResource.PicCaptcha(bm))
            val res = verificationResult.await()
            onLoginStateChanged(LoginResource.None)
            return res
        }

        override suspend fun onSolveSliderCaptcha(bot: Bot, url: String): String? {
            Log.d("parabox", "onSolveSliderCaptcha")
            verificationResult = CompletableDeferred()
//        this.url = url
            onLoginStateChanged(LoginResource.SliderCaptcha(url))
            val res = verificationResult.await()
            onLoginStateChanged(LoginResource.None)
            return res
        }

        override suspend fun onSolveUnsafeDeviceLoginVerify(bot: Bot, url: String): String? {
            Log.d("parabox", "onSolveUnsafeDeviceLoginVerify")
            verificationResult = CompletableDeferred()
//        this.url = url
            onLoginStateChanged(LoginResource.UnsafeDeviceLoginVerify(url))
            val res = verificationResult.await()
            onLoginStateChanged(LoginResource.None)
            return res
        }

        override val isSliderCaptchaSupported: Boolean
            get() = true

    }

    override fun miraiStart() {
        interfaceMessenger?.send(
            Message.obtain(null, ConnKey.MSG_RESPONSE, Bundle().apply {
                putInt("command", ConnKey.MSG_RESPONSE_START_SERVICE)
                putInt("status", ConnKey.SUCCESS)
                putParcelable("value", ServiceStatus.Loading("尝试以默认账户登录"))
            })
        )
        if (isRunning) return
        //miraiMain()
        isRunning = true
    }

    override fun miraiStop() {
        interfaceMessenger?.send(
            Message.obtain(null, ConnKey.MSG_RESPONSE, Bundle().apply {
                putInt("command", ConnKey.MSG_RESPONSE_STOP_SERVICE)
                putInt("status", ConnKey.SUCCESS)
            })
        )
        unRegisterMessageReceiver()
        lifecycleScope.cancel()
        stopSelf()
    }

    override fun miraiLogin() {
        lifecycleScope.launch {
            val secret = withContext(Dispatchers.IO) {
                repository.getSelectedAccount()
            }
            if (secret == null) {
                interfaceMessenger?.send(
                    Message.obtain(null, ConnKey.MSG_RESPONSE, Bundle().apply {
                        putInt("command", ConnKey.MSG_RESPONSE_STOP_SERVICE)
                        putInt("status", ConnKey.FAILURE)
                        putParcelable("value", ServiceStatus.Error("请至少添加并选择一个账户"))
                    })
                )
            } else {
                miraiMain(secret.account, secret.password)
            }
        }
    }

    override fun onLoginStateChanged(resource: LoginResource) {
        interfaceMessenger?.send(
            Message.obtain(null, ConnKey.MSG_RESPONSE, Bundle().apply {
                putInt("command", ConnKey.MSG_RESPONSE_LOGIN)
                putInt("status", ConnKey.SUCCESS)
                putParcelable("value", ServiceStatus.Pause("请遵照提示完成身份验证"))
            })
        )
        interfaceMessenger?.send(
            Message.obtain(null, ConnKey.MSG_COMMAND, Bundle().apply {
                putInt("command", ConnKey.MSG_COMMAND_ON_LOGIN_STATE_CHANGED)
                putInt(
                    "type", when (resource) {
                        is LoginResource.None -> LoginResourceType.None
                        is LoginResource.PicCaptcha -> LoginResourceType.PicCaptcha
                        is LoginResource.SliderCaptcha -> LoginResourceType.SliderCaptcha
                        is LoginResource.UnsafeDeviceLoginVerify -> LoginResourceType.UnsafeDeviceLoginVerify
                    }
                )
                putParcelable("value", resource)
            })
        )
    }

    override fun submitVerificationResult(result: String) {
        mLoginSolver.submitVerificationResult(result)
    }


}