package com.ojhdtapp.miraipluginforparabox.toolkit

import android.app.Service
import android.content.Intent
import android.os.*
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleService

abstract class ParaboxService : LifecycleService() {
    var serviceState = ParaboxKey.STATE_STOP
    lateinit var paraboxMessenger: Messenger
    private var clientMessenger: Messenger? = null
    private var mainAppMessenger: Messenger? = null

    abstract fun onStartParabox()
    abstract fun onStopParabox()
    abstract fun onStateUpdate()
    abstract fun customHandleMessage(msg: Message)
    fun startParabox() {
        onStartParabox()
    }

    fun stopParabox() {
        onStopParabox()
    }

    private fun updateServiceState(state: Int){
        serviceState = state
        noticeStateUpdate()
    }
    fun noticeStateUpdate(){

    }

    fun receiveMessage() {

    }
    abstract fun onSendMessage()

    private fun sendResponse(){
        
        coreSendResponse()
    }
    private fun coreSendResponse(
        isSuccess: Boolean,
        target: Int,
        timestamp: Long,
        command: Int,
        result: ParaboxCommandResult,
        extra: Bundle = Bundle()
    ) {
        when (target) {
            ParaboxKey.TYPE_MAIN_APP -> {}
            ParaboxKey.TYPE_CLIENT -> {
                val msg = Message.obtain(null, command, ParaboxKey.TYPE_CLIENT, 0, extra.apply {
                    putBoolean("isSuccess", isSuccess)
                    putLong("timestamp", timestamp)
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
            val obj = msg.obj
            when (msg.arg1) {
                ParaboxKey.TYPE_CLIENT -> {
                    clientMessenger = msg.replyTo
                    when (msg.what) {
                        ParaboxKey.COMMAND_START_SERVICE -> {

                        }
                        ParaboxKey.COMMAND_STOP_SERVICE -> {}
                        else -> customHandleMessage(msg)
                    }
                }
                ParaboxKey.TYPE_MAIN_APP -> {
                    mainAppMessenger = msg.replyTo
                }
            }
        }
    }
}


