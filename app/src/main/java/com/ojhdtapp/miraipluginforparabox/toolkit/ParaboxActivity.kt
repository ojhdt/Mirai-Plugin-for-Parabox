package com.ojhdtapp.miraipluginforparabox.toolkit

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ojhdtapp.miraipluginforparabox.core.util.CompletableDeferredWithTag
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

abstract class ParaboxActivity<T>(private val serviceClass: Class<T>) : AppCompatActivity() {
    abstract fun onParaboxServiceConnected()
    abstract fun onParaboxServiceDisconnected()

    var paraboxService: Messenger? = null
    private lateinit var client: Messenger
    private lateinit var paraboxServiceConnection: ServiceConnection

    var deferredMap = mutableMapOf<Long, CompletableDeferredWithTag<Long, ParaboxCommandResult>>()

    /*/
    推荐于onStart运行
     */
    fun bindParaboxService() {
        val intent = Intent(this, serviceClass)
        startService(intent)
        bindService(intent, paraboxServiceConnection, BIND_AUTO_CREATE)
    }

    /*/
    推荐于onStop执行
     */
    fun stopParaboxService() {
        if (paraboxService != null) {
            unbindService(paraboxServiceConnection)
        }
    }

    fun sendCommand(command: Int, onResult: (ParaboxCommandResult) -> Unit) {
        lifecycleScope.launch {
            val timestamp = System.currentTimeMillis()
            try {
                withTimeout(3000) {
                    val deferred = CompletableDeferredWithTag<Long, ParaboxCommandResult>(timestamp)
                    deferredMap[timestamp] = deferred
                    coreSendCommand(timestamp, command)
                    deferred.await().also {
                        onResult(
                            ParaboxCommandResult.Success(
                                command,
                                timestamp
                            )
                        )
                    }
                }
            } catch (e: TimeoutCancellationException) {
                deferredMap[timestamp]?.cancel()
                onResult(
                    ParaboxCommandResult.Fail(
                        command,
                        timestamp,
                        ParaboxKey.ERROR_TIMEOUT
                    )
                )
            } catch (e: RemoteException) {
                deferredMap[timestamp]?.cancel()
                onResult(
                    ParaboxCommandResult.Fail(
                        command,
                        timestamp,
                        ParaboxKey.ERROR_DISCONNECTED
                    )
                )
            }
        }
    }

    private fun coreSendCommand(timestamp: Long, command: Int, extra: Bundle = Bundle()) {
        if (paraboxService == null) {
            deferredMap[timestamp]?.complete(
                timestamp,
                ParaboxCommandResult.Fail(
                    command, timestamp,
                    ParaboxKey.ERROR_DISCONNECTED
                )
            )
        } else {
            val msg = Message.obtain(null, command, ParaboxKey.TYPE_CLIENT, 0, extra.apply {
                putParcelable("metadata", ParaboxMetadata(
                    command = command,
                    timestamp = timestamp,
                    type = ParaboxKey.TYPE_CLIENT
                ))
            }).apply {
                replyTo = client
            }
            paraboxService!!.send(msg)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        client = Messenger(ParaboxServiceHandler())
        paraboxServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
                paraboxService = Messenger(p1)
                onParaboxServiceConnected()
            }

            override fun onServiceDisconnected(p0: ComponentName?) {
                paraboxService = null
                onParaboxServiceDisconnected()
            }

        }
    }

    inner class ParaboxServiceHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val obj = msg.obj as Bundle
            val sendTimestamp = obj.getParcelable<ParaboxMetadata>("metadata")!!.timestamp
            val isSuccess = obj.getBoolean("isSuccess")
            val result = if (isSuccess) {
                obj.getParcelable<ParaboxCommandResult.Success>("result")
            } else {
                obj.getParcelable<ParaboxCommandResult.Fail>("result")
            }
            result?.let {
                deferredMap[sendTimestamp]?.complete(sendTimestamp, it)
            }
            super.handleMessage(msg)
        }
    }
}