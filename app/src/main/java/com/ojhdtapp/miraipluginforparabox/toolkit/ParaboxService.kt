package com.ojhdtapp.miraipluginforparabox.toolkit

import android.app.Service
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleService

abstract class ParaboxService : LifecycleService() {
    var isRunning = false
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

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }
}


