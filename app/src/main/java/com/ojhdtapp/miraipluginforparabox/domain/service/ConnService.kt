package com.ojhdtapp.miraipluginforparabox.domain.service

import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.lifecycle.LifecycleService

class ConnService : LifecycleService() {
    private lateinit var sMessenger: Messenger
    private lateinit var cMessenger: Messenger

    override fun onCreate() {
        super.onCreate()
        Log.d("parabox", "service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("parabox", "service started")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d("parabox", "service bound")
        super.onBind(intent)
        sMessenger = Messenger(ConnHandler())
        return sMessenger.binder
    }

    inner class ConnHandler : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            cMessenger = msg.replyTo
            val str = (msg.obj as Bundle).getString("str") ?: "error"
            Log.d("parabox", "message from cliect: $str")
            try {
                cMessenger.send(Message().apply {
                    obj = Bundle().apply {
                        putString("str", "$str--connected!")
                    }
                })
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }
    }
}