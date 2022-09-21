package com.ojhdtapp.miraipluginforparabox.toolkit

import android.app.Service
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleService

abstract class ParaboxService : LifecycleService() {
    var isRunning = false
    lateinit var paraboxMessenger : Messenger
    private var interfaceMessenger : Messenger? = null

    abstract fun onStartParabox()
    abstract fun onStopParabox()
    fun startParabox(){
        onStartParabox()
    }
    fun stopParabox(){
        onStopParabox()
    }
    fun receiveMessage(){

    }
    abstract fun onSendMessage()

    private fun coreResponseCommand(isSuccess : Boolean ,timestamp: Long, command: Int, extra: Bundle = Bundle()){
        
    }

    override fun onCreate() {
        paraboxMessenger = Messenger(CommandHandler())
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder {
        super.onBind(intent)
        return paraboxMessenger.binder
    }

    inner class CommandHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when(msg.what){
                ParaboxCommand.COMMAND_START_SERVICE -> {}
                ParaboxCommand.COMMAND_STOP_SERVICE -> {}
            }
        }
    }
}


