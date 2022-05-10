package com.ojhdtapp.miraipluginforparabox.domain.service

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.*
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.ojhdtapp.miraipluginforparabox.domain.util.LoginResource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.network.LoginFailedException
import net.mamoe.mirai.utils.LoginSolver
import kotlin.coroutines.suspendCoroutine

class ConnService : LifecycleService() {
    companion object {
        lateinit var instance: ConnService

        fun stop() {
            instance.stopSelf()
        }

    }

    val mLoginSolver = AndroidLoginSolver()

    private val sMessenger: Messenger = Messenger(ConnHandler())
    private var cMessenger: Messenger? = null

    private fun miraiMain(accountNum: Long, passwd: String) {
        val bot = BotFactory.newBot(accountNum, passwd) {
            parentCoroutineContext = lifecycleScope.coroutineContext
            loginSolver = mLoginSolver
        }
        lifecycleScope.launch {
            try {
                bot.login()
            } catch (e: LoginFailedException) {
                e.printStackTrace()
            }

        }
        GlobalEventChannel.parentScope(lifecycleScope).subscribeAlways<GroupMessageEvent> { event ->
            Log.d("parabox", "${event.senderName}:${event.message}")
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d("parabox", "service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("parabox", "service started")
        val obj = intent?.extras?.getBundle("data") ?: Bundle()
        val accountNum = obj.getLong("accountNum")
        val passwd = obj.getString("passwd") ?: ""
        Log.d("parabox", "$accountNum - $passwd")
        miraiMain(accountNum, passwd)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d("parabox", "service bound")
        super.onBind(intent)
        return sMessenger.binder
    }

    fun send(str: String) {
        if (cMessenger == null) {
            throw RemoteException("not connected")
        }
        try {
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
            super.handleMessage(msg)
            cMessenger = msg.replyTo
            val str = (msg.obj as Bundle).getString("str") ?: "error"
            Log.d("parabox", "message from cliect: $str")

        }
    }
}