package com.ojhdtapp.miraipluginforparabox.domain.service

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.*
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.ojhdtapp.miraipluginforparabox.MainActivity
import com.ojhdtapp.miraipluginforparabox.core.util.NotificationUtil
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
import net.mamoe.mirai.utils.BotConfiguration
import net.mamoe.mirai.utils.LoginSolver
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine
import kotlin.system.exitProcess

@AndroidEntryPoint
class ConnService : LifecycleService() {
    @Inject
    lateinit var repository: MainRepository
    lateinit var notificationUtil: NotificationUtil

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
            protocol = BotConfiguration.MiraiProtocol.ANDROID_PHONE
        }
        try {
            bot.login()
            isRunning = true
            val version = Bot::class.java.`package`?.implementationVersion ?: "unknown"
            interfaceMessenger?.send(
                Message.obtain(null, ConnKey.MSG_RESPONSE, Bundle().apply {
                    putInt("command", ConnKey.MSG_RESPONSE_LOGIN)
                    putInt("status", ConnKey.SUCCESS)
                    putLong("timestamp", mLoginSolver.timestamp)
                    putParcelable("value", ServiceStatus.Running("Mirai Core - $version"))
                })
            )
            notificationUtil.updateForegroundServiceNotification("服务正常运行", "Mirai Core - $version")
            registerMessageReceiver()
            repository.getSelectedAccount()?.let {
                repository.addNewAccount(it.apply {
                    avatarUrl = bot.avatarUrl
                })
            }
        } catch (e: LoginFailedException) {
            interfaceMessenger?.send(
                Message.obtain(null, ConnKey.MSG_RESPONSE, Bundle().apply {
                    putInt("command", ConnKey.MSG_RESPONSE_LOGIN)
                    putInt("status", ConnKey.FAILURE)
                    putLong("timestamp", mLoginSolver.timestamp)
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
        sMessenger = Messenger(ConnHandler())
        notificationUtil = NotificationUtil(this)
        super.onCreate()
//        mLoginSolver = AndroidLoginSolver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("parabox", "service started")
        notificationUtil.startForegroundService()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return sMessenger.binder
    }

    override fun onDestroy() {
        Log.d("parabox", "on destroy")
        notificationUtil.cancelForegroundServiceNotification()
        super.onDestroy()
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
                    val timestamp = (msg.obj as Bundle).getLong("timestamp", -1L)
                    when ((msg.obj as Bundle).getInt("command", -1)) {
                        ConnKey.MSG_COMMAND_START_SERVICE -> {
                            miraiStart(timestamp)
                        }
                        ConnKey.MSG_COMMAND_STOP_SERVICE -> {
                            miraiStop(timestamp)
                        }
                        ConnKey.MSG_COMMAND_LOGIN -> {
                            miraiLogin(timestamp)
                        }
                        ConnKey.MSG_COMMAND_SUBMIT_VERIFICATION_RESULT -> {
                            (msg.obj as Bundle).getString("value")?.let {
                                submitVerificationResult(it, timestamp)
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

    inner class AndroidLoginSolver(var timestamp: Long) : LoginSolver() {
        private lateinit var verificationResult: CompletableDeferred<String>

        fun submitVerificationResult(result: String, timestampWhenSubmit: Long) {
            timestamp = timestampWhenSubmit // update the tag to the latest waiting
            verificationResult.complete(result)
        }

        override suspend fun onSolvePicCaptcha(bot: Bot, data: ByteArray): String {
            val newTimeStamp = System.currentTimeMillis()
            Log.d("parabox", "onSolvePicCaptcha")
            verificationResult = CompletableDeferred()
//        captchaData = data
            val bm = BitmapFactory.decodeByteArray(data, 0, data.size)
            onLoginStateChanged(LoginResource.PicCaptcha(bm, newTimeStamp), timestamp, newTimeStamp)
            val res = verificationResult.await()
            onLoginStateChanged(LoginResource.None, timestamp, newTimeStamp)
            return res
        }

        override suspend fun onSolveSliderCaptcha(bot: Bot, url: String): String? {
            val newTimeStamp = System.currentTimeMillis()
            Log.d("parabox", "onSolveSliderCaptcha")
            verificationResult = CompletableDeferred()
//        this.url = url
            onLoginStateChanged(
                LoginResource.SliderCaptcha(url, newTimeStamp),
                timestamp,
                newTimeStamp
            )
            val res = verificationResult.await()
            onLoginStateChanged(LoginResource.None, timestamp, newTimeStamp)
            return res
        }

        override suspend fun onSolveUnsafeDeviceLoginVerify(bot: Bot, url: String): String? {
            val newTimeStamp = System.currentTimeMillis()
            Log.d("parabox", "onSolveUnsafeDeviceLoginVerify")
            verificationResult = CompletableDeferred()
//        this.url = url
            onLoginStateChanged(
                LoginResource.UnsafeDeviceLoginVerify(url, newTimeStamp),
                timestamp,
                newTimeStamp
            )
            val res = verificationResult.await()
            onLoginStateChanged(LoginResource.None, timestamp, newTimeStamp)
            return res
        }

        override val isSliderCaptchaSupported: Boolean
            get() = true

    }

    fun miraiStart(timestamp: Long) {
        interfaceMessenger?.send(
            Message.obtain(null, ConnKey.MSG_RESPONSE, Bundle().apply {
                putInt("command", ConnKey.MSG_RESPONSE_START_SERVICE)
                putInt("status", ConnKey.SUCCESS)
                putLong("timestamp", timestamp)
                putParcelable(
                    "value",
                    if (isRunning) ServiceStatus.Error("服务正在运行，请勿重复启动") else ServiceStatus.Loading("尝试以默认账户登录")
                )
            })
        )
    }

    fun miraiStop(timestamp: Long) {
        // movement
        unRegisterMessageReceiver()
        bot.close()
        lifecycleScope.cancel()
        // res
        isRunning = false
        interfaceMessenger?.send(
            Message.obtain(null, ConnKey.MSG_RESPONSE, Bundle().apply {
                putInt("command", ConnKey.MSG_RESPONSE_STOP_SERVICE)
                putInt("status", ConnKey.SUCCESS)
                putLong("timestamp", timestamp)
                putParcelable("value", ServiceStatus.Stop)
            })
        )
        Log.d("parabox", "before stop self")
        stopSelf()
    }

    fun miraiLogin(timestamp: Long) {
        lifecycleScope.launch {
            val secret = withContext(Dispatchers.IO) {
                repository.getSelectedAccount()
            }
            if (secret == null) {
                interfaceMessenger?.send(
                    Message.obtain(null, ConnKey.MSG_RESPONSE, Bundle().apply {
                        putInt("command", ConnKey.MSG_RESPONSE_STOP_SERVICE)
                        putInt("status", ConnKey.FAILURE)
                        putLong("timestamp", timestamp)
                        putParcelable("value", ServiceStatus.Error("请至少添加并选择一个账户"))
                    })
                )
            } else {
                mLoginSolver = AndroidLoginSolver(timestamp)
                miraiMain(secret.account, secret.password)
            }
        }
    }

    fun onLoginStateChanged(resource: LoginResource, timestamp: Long, newTimeStamp: Long) {
        Log.d("parabox", "timestamp need to replace original response: $timestamp")
        Log.d("parabox", "newTimestamp use to check resource and deffered: $timestamp")
        if (resource !is LoginResource.None) {
            interfaceMessenger?.send(
                Message.obtain(null, ConnKey.MSG_RESPONSE, Bundle().apply {
                    putInt("command", ConnKey.MSG_RESPONSE_LOGIN)
                    putInt("status", ConnKey.SUCCESS)
                    putLong("timestamp", timestamp) // The reason we need old timestamp here
                    putParcelable("value", ServiceStatus.Pause("请遵照提示完成身份验证", newTimeStamp))
                })
            )
        }
        interfaceMessenger?.send(
            Message.obtain(null, ConnKey.MSG_COMMAND, Bundle().apply {
                putInt("command", ConnKey.MSG_COMMAND_ON_LOGIN_STATE_CHANGED)
                putInt("status", ConnKey.SUCCESS)
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

    fun submitVerificationResult(result: String, timestamp: Long) {
        mLoginSolver.submitVerificationResult(result, timestamp)
    }
}