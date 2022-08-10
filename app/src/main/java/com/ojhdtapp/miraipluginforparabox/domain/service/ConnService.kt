package com.ojhdtapp.miraipluginforparabox.domain.service

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.*
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.ojhdtapp.miraipluginforparabox.core.MIRAI_CORE_VERSION
import com.ojhdtapp.miraipluginforparabox.core.util.DataStoreKeys
import com.ojhdtapp.miraipluginforparabox.core.util.NotificationUtilForService
import com.ojhdtapp.miraipluginforparabox.core.util.dataStore
import com.ojhdtapp.parabox.dto.MessageDto
import com.ojhdtapp.miraipluginforparabox.domain.model.PluginConnection
import com.ojhdtapp.miraipluginforparabox.domain.model.Profile
import com.ojhdtapp.miraipluginforparabox.domain.repository.MainRepository
import com.ojhdtapp.miraipluginforparabox.domain.util.LoginResource
import com.ojhdtapp.miraipluginforparabox.domain.util.LoginResourceType
import com.ojhdtapp.miraipluginforparabox.domain.util.MiraiProtocol
import com.ojhdtapp.miraipluginforparabox.domain.util.ServiceStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import net.mamoe.mirai.Bot
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.event.Listener
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.subscribeAlways
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.SingleMessage
import net.mamoe.mirai.network.LoginFailedException
import net.mamoe.mirai.utils.BotConfiguration
import net.mamoe.mirai.utils.DeviceInfo
import net.mamoe.mirai.utils.LoginSolver
import javax.inject.Inject
import kotlin.properties.Delegates

@AndroidEntryPoint
class ConnService : LifecycleService() {
    @Inject
    lateinit var repository: MainRepository
    lateinit var notificationUtil: NotificationUtilForService

    private var bot: Bot? = null
    private var friendMessageEventListener: Listener<FriendMessageEvent>? = null
    private var groupMessageEventListener: Listener<GroupMessageEvent>? = null
    private lateinit var mLoginSolver: AndroidLoginSolver
    private lateinit var sMessenger: Messenger
    private var cMessenger: Messenger? = null
    private var interfaceMessenger: Messenger? = null

    private var isRunning = false

    private var miraiConnectionType by Delegates.notNull<Int>()

    private suspend fun miraiMain(accountNum: Long, passwd: String, miraiDeviceInfo: DeviceInfo) {
        val isContactCacheEnabled =
            dataStore.data.first()[DataStoreKeys.CONTACT_CACHE] ?: false
        val selectedProtocol =
            when (dataStore.data.first()[DataStoreKeys.PROTOCOL] ?: MiraiProtocol.Phone) {
                MiraiProtocol.Phone -> BotConfiguration.MiraiProtocol.ANDROID_PHONE
                MiraiProtocol.Pad -> BotConfiguration.MiraiProtocol.ANDROID_PAD
                MiraiProtocol.Watch -> BotConfiguration.MiraiProtocol.ANDROID_WATCH
                MiraiProtocol.IPad -> BotConfiguration.MiraiProtocol.IPAD
                MiraiProtocol.MacOS -> BotConfiguration.MiraiProtocol.MACOS
                else -> BotConfiguration.MiraiProtocol.ANDROID_PHONE
            }
        bot = BotFactory.newBot(accountNum, passwd) {
            loginSolver = mLoginSolver
            cacheDir = getExternalFilesDir("cache")!!.absoluteFile
            protocol = selectedProtocol
            if (isContactCacheEnabled) enableContactCache()
            deviceInfo = {bot -> miraiDeviceInfo}
        }
        try {
            bot?.login()
            isRunning = true
            val version = MIRAI_CORE_VERSION
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
                    avatarUrl = bot?.avatarUrl
                })
            }
            repository.insertDeviceInfo(miraiDeviceInfo)
        } catch (e: LoginFailedException) {
            interfaceMessenger?.send(
                Message.obtain(null, ConnKey.MSG_RESPONSE, Bundle().apply {
                    putInt("command", ConnKey.MSG_RESPONSE_LOGIN)
                    putInt("status", ConnKey.FAILURE)
                    putLong("timestamp", mLoginSolver.timestamp)
                    putParcelable("value", ServiceStatus.Error("登陆失败，密码与账户不匹配"))
                })
            )
        } catch (e: IllegalStateException) {
            interfaceMessenger?.send(
                Message.obtain(null, ConnKey.MSG_RESPONSE, Bundle().apply {
                    putInt("command", ConnKey.MSG_RESPONSE_LOGIN)
                    putInt("status", ConnKey.FAILURE)
                    putLong("timestamp", mLoginSolver.timestamp)
                    putParcelable("value", ServiceStatus.Error("登陆失败，未知错误"))
                })
            )
        }

    }

    private fun registerMessageReceiver() {
        friendMessageEventListener =
            bot?.eventChannel!!.parentScope(lifecycleScope)
                .subscribeAlways<net.mamoe.mirai.event.events.FriendMessageEvent> { event ->
                    Log.d("aaa", "${event.senderName}:${event.message}")
                    event.subject.sendMessage("Hello from mirai!")
                    val messageContents = event.message.map {
                        when (it) {
                            is PlainText -> com.ojhdtapp.miraipluginforparabox.domain.model.message_content.PlainText(
                                it.content
                            )
                            is Image -> com.ojhdtapp.miraipluginforparabox.domain.model.message_content.Image(
                                it.queryUrl()
                            )
                            else -> com.ojhdtapp.miraipluginforparabox.domain.model.message_content.PlainText(
                                "不支持的内容"
                            )
                        }
                    }
                    val profile = Profile(
                        name = event.senderName,
                        avatar = event.sender.avatarUrl
                    )
                    val pluginConnection = PluginConnection(
                        connectionType = miraiConnectionType,
                        objectId = "${miraiConnectionType}${event.subject.id}".toLong(),
                        id = event.subject.id
                    )
                    val dto = MessageDto(
                        contents = messageContents,
                        profile = profile,
                        subjectProfile = profile,
                        timestamp = (event.time * 1000).toLong(),
                        pluginConnection = pluginConnection
                    )
                    sendMessageToMainApp(dto)
                }
        groupMessageEventListener =
            bot?.eventChannel!!.parentScope(lifecycleScope).subscribeAlways { event ->
                val messageContents = event.message.map {
                    when (it) {
                        is PlainText -> com.ojhdtapp.miraipluginforparabox.domain.model.message_content.PlainText(
                            it.content
                        )
                        is Image -> com.ojhdtapp.miraipluginforparabox.domain.model.message_content.Image(
                            it.queryUrl()
                        )
                        else -> com.ojhdtapp.miraipluginforparabox.domain.model.message_content.PlainText(
                            "不支持的内容"
                        )
                    }
                }
                val senderProfile = Profile(
                    name = event.senderName,
                    avatar = event.sender.avatarUrl
                )
                val groupProfile = Profile(
                    name = event.group.name,
                    avatar = event.group.avatarUrl
                )
                val pluginConnection = PluginConnection(
                    connectionType = miraiConnectionType,
                    objectId = "${miraiConnectionType}${event.subject.id}".toLong(),
                    id = event.subject.id
                )
                val dto = MessageDto(
                    contents = messageContents,
                    profile = senderProfile,
                    subjectProfile = groupProfile,
                    timestamp = (event.time * 1000).toLong(),
                    pluginConnection = pluginConnection
                )
                sendMessageToMainApp(dto)
            }
    }

    private fun unRegisterMessageReceiver() {
        if (friendMessageEventListener?.isActive == true) {
            friendMessageEventListener!!.complete()
            friendMessageEventListener = null
        }
        if (groupMessageEventListener?.isActive == true) {
            groupMessageEventListener!!.complete()
            groupMessageEventListener = null
        }
    }

    private fun sendMessageToMainApp(dto: MessageDto) {
        cMessenger?.send(
            Message.obtain(null, ConnKey.MSG_MESSAGE, Bundle().apply {
                putInt("command", ConnKey.MSG_MESSAGE_RECEIVE)
                putInt("status", ConnKey.SUCCESS)
                putParcelable("value", dto)
            })
        )
    }

    override fun onCreate() {
        sMessenger = Messenger(ConnHandler())
        notificationUtil = NotificationUtilForService(this)
        miraiConnectionType = packageManager.getApplicationInfo(
            this@ConnService.packageName,
            PackageManager.GET_META_DATA
        ).metaData.getInt("connection_type")
        super.onCreate()
//        mLoginSolver = AndroidLoginSolver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("parabox", "service started")
        lifecycleScope.launch {
            val isForegroundServiceEnabled =
                dataStore.data.first()[DataStoreKeys.FOREGROUND_SERVICE] ?: false
            if (isForegroundServiceEnabled) {
                notificationUtil.startForegroundService()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return sMessenger.binder
    }

    override fun onDestroy() {
        Log.d("parabox", "on destroy")
        notificationUtil.stopForegroundService()
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
                    when ((msg.obj as Bundle).getInt("command", -1)) {
                        ConnKey.MSG_MESSAGE_CHECK_RUNNING_STATUS -> {
                            checkRunningStatus()
                        }
                        ConnKey.MSG_MESSAGE_TRY_AUTO_LOGIN -> {
                            tryAutoLogin()
                        }
                    }
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
                    if (isRunning) {
                        ServiceStatus.Error("服务正在运行，请勿重复启动")
                    } else {
                        notificationUtil.updateForegroundServiceNotification("服务正在启动", "尝试以默认账户登录")
                        ServiceStatus.Loading("尝试以默认账户登录")
                    }
                )
            })
        )
    }

    fun miraiStop(timestamp: Long) {
        // movement
        unRegisterMessageReceiver()
        bot?.close()
        bot = null
        lifecycleScope.cancel()
        // res
        isRunning = false
        notificationUtil.stopForegroundService()
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
            val deviceInfo = withContext(Dispatchers.IO){
                repository.getDeviceInfo()
            }?: DeviceInfo.random()
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
                miraiMain(secret.account, secret.password, deviceInfo)
            }
        }
    }

    fun onLoginStateChanged(resource: LoginResource, timestamp: Long, newTimeStamp: Long) {
        Log.d("parabox", "timestamp need to replace original response: $timestamp")
        Log.d("parabox", "newTimestamp use to check resource and deffered: $timestamp")
        if (resource !is LoginResource.None) {
            notificationUtil.updateForegroundServiceNotification("等待事务处理", "请遵照提示完成身份验证")
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

    fun checkRunningStatus() {
        cMessenger?.send(
            Message.obtain(null, ConnKey.MSG_RESPONSE, Bundle().apply {
                putInt("command", ConnKey.MSG_RESPONSE_CHECK_RUNNING_STATUS)
                putInt("status", ConnKey.SUCCESS)
                putBoolean("value", isRunning)
            })
        )
    }

    fun tryAutoLogin() {
        lifecycleScope.launch {
            val isAutoLoginEnabled =
                dataStore.data.first()[DataStoreKeys.AUTO_LOGIN] ?: false
            TODO("Auto Login")
        }
    }
}