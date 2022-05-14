package com.ojhdtapp.miraipluginforparabox.domain.service

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.*
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.ojhdtapp.miraipluginforparabox.domain.service.ConnKey
import com.ojhdtapp.miraipluginforparabox.domain.util.LoginResource
import com.ojhdtapp.miraipluginforparabox.domain.util.LoginResourceType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.Listener
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.network.LoginFailedException
import net.mamoe.mirai.utils.LoginSolver
import java.io.File
import kotlin.coroutines.suspendCoroutine
import kotlin.system.exitProcess

class ConnService : ConnCommandInterface, LifecycleService() {

    private lateinit var bot: Bot
    private var listener: Listener<FriendMessageEvent>? = null
    private lateinit var mLoginSolver: AndroidLoginSolver
    private lateinit var sMessenger: Messenger
    private var cMessenger: Messenger? = null
    private var interfaceMessenger: Messenger? = null

    private var isRunning = false

    private fun miraiMain(accountNum: Long, passwd: String) {
        Log.d("parabox", "$accountNum - $passwd")
        bot = BotFactory.newBot(accountNum, passwd) {
            loginSolver = mLoginSolver
            cacheDir = getExternalFilesDir("cache")!!.absoluteFile
        }
        lifecycleScope.launch {
            try {
                bot.login()
                registerMessageReceiver()
            } catch (e: LoginFailedException) {
            }
        }
    }

    private fun registerMessageReceiver() {
        Log.d("parabox", "receiver registered")
        lifecycleScope.launch {
            listener =
                bot.eventChannel.subscribeAlways<net.mamoe.mirai.event.events.FriendMessageEvent> { event ->
                    Log.d("aaa", "${event.senderName}:${event.message}")
                    event.subject.sendMessage("Hello from mirai!")
                }
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
                            Log.d("parabox", "received")
                            miraiMain("2371065280".toLong(), "b20011007")
                        }
                        ConnKey.MSG_COMMAND_STOP_SERVICE -> {
                            miraiStop()
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
        if (isRunning) return
        //miraiMain()
        isRunning = true
    }

    override fun miraiStop() {
        unRegisterMessageReceiver()
        lifecycleScope.cancel()
        stopSelf()
    }

    override fun onLoginStateChanged(resource: LoginResource) {
        Log.d("parabox", (interfaceMessenger == null).toString())
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