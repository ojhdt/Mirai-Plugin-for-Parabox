package com.ojhdtapp.miraipluginforparabox.domain.service

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.*
import android.os.Message
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.ojhdtapp.messagedto.PluginConnection
import com.ojhdtapp.messagedto.Profile
import com.ojhdtapp.messagedto.ReceiveMessageDto
import com.ojhdtapp.messagedto.SendMessageDto
import com.ojhdtapp.miraipluginforparabox.core.MIRAI_CORE_VERSION
import com.ojhdtapp.miraipluginforparabox.core.util.DataStoreKeys
import com.ojhdtapp.miraipluginforparabox.core.util.NotificationUtilForService
import com.ojhdtapp.miraipluginforparabox.core.util.dataStore
import com.ojhdtapp.miraipluginforparabox.data.local.entity.MiraiMessageEntity
import com.ojhdtapp.miraipluginforparabox.domain.repository.MainRepository
import com.ojhdtapp.miraipluginforparabox.domain.util.*
import dagger.hilt.android.AndroidEntryPoint
import io.github.kasukusakura.silkcodec.AudioToSilkCoder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import net.mamoe.mirai.Bot
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.contact.BotIsBeingMutedException
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.MessageTooLargeException
import net.mamoe.mirai.event.Listener
import net.mamoe.mirai.event.events.EventCancelledException
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.MessageChain.Companion.serializeToJsonString
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.network.LoginFailedException
import net.mamoe.mirai.utils.BotConfiguration
import net.mamoe.mirai.utils.DeviceInfo
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.LoginSolver
import java.io.*
import java.util.*
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class ConnService : LifecycleService() {
    external fun stringFromJNI(): String
    companion object {
        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
        var connectionType = 0
    }

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
    private var receiptMap = mutableMapOf<Long, MessageReceipt<Contact>>()

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
            deviceInfo = { bot -> miraiDeviceInfo }
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
                .subscribeAlways<FriendMessageEvent> { event ->
//                    Log.d("aaa", "${event.senderName}:${event.message}")
//                    event.subject.sendMessage("Hello from mirai!")
                    val messageContents =
                        event.message.toMessageContentList(bot = bot, repository = repository)
                    val messageId = getMessageId(event.message.ids)
                    messageId?.let {
                        lifecycleScope.launch(Dispatchers.IO) {
                            repository.insertMiraiMessage(
                                MiraiMessageEntity(
                                    it,
                                    event.message.serializeToMiraiCode(),
                                    event.message.serializeToJsonString()
                                )
                            )

                        }
                    }

                    val profile = Profile(
                        name = event.senderName,
                        avatar = event.sender.avatarUrl
                    )
                    val pluginConnection = PluginConnection(
                        connectionType = connectionType,
                        objectId = "${connectionType}${SendTargetType.USER}${event.subject.id}".toLong(),
                        id = event.subject.id
                    )
                    val dto = ReceiveMessageDto(
                        contents = messageContents,
                        profile = profile,
                        subjectProfile = profile,
                        timestamp = "${event.time}000".toLong(),
                        messageId = messageId,
                        pluginConnection = pluginConnection
                    )
                    sendMessageToMainApp(dto)
                }
        groupMessageEventListener =
            bot?.eventChannel!!.parentScope(lifecycleScope).subscribeAlways { event ->
                val messageContents = event.message.toMessageContentList(
                    group = event.group,
                    bot = bot,
                    repository = repository
                )
                val messageId = getMessageId(event.message.ids)
                messageId?.let {
                    lifecycleScope.launch(Dispatchers.IO) {
                        repository.insertMiraiMessage(
                            MiraiMessageEntity(
                                it,
                                event.message.serializeToMiraiCode(),
                                event.message.serializeToJsonString()
                            )
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
                    connectionType = connectionType,
                    objectId = "${connectionType}${SendTargetType.GROUP}${event.subject.id}".toLong(),
                    id = event.subject.id
                )
                val dto = ReceiveMessageDto(
                    contents = messageContents,
                    profile = senderProfile,
                    subjectProfile = groupProfile,
                    timestamp = "${event.time}000".toLong(),
                    messageId = messageId,
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

    private fun sendMessageToMainApp(dto: ReceiveMessageDto) {
        cMessenger?.send(
            Message.obtain(null, ConnKey.MSG_MESSAGE).apply {
                data = Bundle().apply {
                    putParcelable("value", dto)
                }
                obj = Bundle().apply {
                    putInt("command", ConnKey.MSG_MESSAGE_RECEIVE)
                    putInt("status", ConnKey.SUCCESS)
                }
            }
//            Message.obtain(null, ConnKey.MSG_MESSAGE, Bundle().apply {
//                putInt("command", ConnKey.MSG_MESSAGE_RECEIVE)
//                putInt("status", ConnKey.SUCCESS)
//                putParcelable("value", dto)
//            })
        )
    }

    private fun sendMessageSendStateToMainApp(messageId: Long, stateSuccess: Boolean) {
        cMessenger?.send(
            Message.obtain(null, ConnKey.MSG_RESPONSE).apply {
                obj = Bundle().apply {
                    putInt("command", ConnKey.MSG_RESPONSE_MESSAGE_SEND)
                    putInt("status", ConnKey.SUCCESS)
                    putLong("message_id", messageId)
                    putBoolean("value", stateSuccess)
                }
            }
        )
    }

    private fun sendMessageRecallStateToMainApp(stateSuccess: Boolean, messageId: Long) {
        cMessenger?.send(
            Message.obtain(null, ConnKey.MSG_RESPONSE).apply {
                obj = Bundle().apply {
                    putInt("command", ConnKey.MSG_RESPONSE_MESSAGE_RECALL)
                    putInt("status", ConnKey.SUCCESS)
                    putBoolean("value", stateSuccess)
                    putLong("message_id", messageId)
                }
            }
        )
    }

    private fun sendMessage(dto: SendMessageDto) {
        val messageId = dto.messageId
        lifecycleScope.launch {
            try {
                val timestamp = dto.timestamp
                val targetId = dto.pluginConnection.id
                val targetType = dto.pluginConnection.objectId.toString().substring(3, 4).toInt()
                val targetContact = when (targetType) {
                    SendTargetType.USER -> bot?.getFriendOrFail(targetId)
                    SendTargetType.GROUP -> bot?.getGroupOrFail(targetId)
                    else -> {
                        throw NoSuchElementException("wrong id")
                    }
                }
                targetContact?.let { contact ->
                    val contents = dto.contents
                    val messageChain = buildMessageChain {
                        contents.map {
                            when (it) {
                                is com.ojhdtapp.messagedto.message_content.PlainText -> add(
                                    PlainText(it.text)
                                )
                                is com.ojhdtapp.messagedto.message_content.Image -> {
//                                    it.sendIntent?.data?.also { uri ->
//                                        val inputPFD: ParcelFileDescriptor? =
//                                            contentResolver.openFileDescriptor(uri, "r")
//                                        val fd = inputPFD!!.fileDescriptor
//                                        val inputStream = FileInputStream(fd)
//                                        inputStream.use { stream ->
//                                            stream.toExternalResource().use { resource ->
//                                                contact.uploadImage(resource).also {
//                                                    add(it)
//                                                }
//                                            }
//                                        }
//                                        inputPFD.close()
//                                    }
                                    it.uri?.let { uri ->
                                        val inputPFD: ParcelFileDescriptor? =
                                            contentResolver.openFileDescriptor(uri, "r")
                                        val fd = inputPFD!!.fileDescriptor
                                        val inputStream = FileInputStream(fd)
                                        inputStream.use { stream ->
                                            stream.toExternalResource().use { resource ->
                                                contact.uploadImage(resource).also {
                                                    add(it)
                                                }
                                            }
                                        }
                                        inputPFD.close()
                                    }
                                }
                                is com.ojhdtapp.messagedto.message_content.At -> add(At(it.target))
                                is com.ojhdtapp.messagedto.message_content.AtAll -> add(AtAll)
                                is com.ojhdtapp.messagedto.message_content.Audio -> {
                                    it.uri?.let { uri ->
                                        val inputPFD: ParcelFileDescriptor? =
                                            contentResolver.openFileDescriptor(uri, "r")
                                        val fd = inputPFD!!.fileDescriptor
                                        val inputStream = FileInputStream(fd)

//                                        val silkCoder = AudioToSilkCoder(Executors.newCachedThreadPool())
//                                        val silkPath = File(this@ConnService.externalCacheDir, "out.silk")
//                                        BufferedOutputStream(FileOutputStream(silkPath)).use { fso ->
//                                            inputStream.use { fsi ->
//                                                silkCoder.connect(
//                                                    "ffmpeg",
//                                                    fsi,
//                                                    fso
//                                                )
//                                                fso
//                                            }
//                                        }
//                                        silkPath.inputStream().use { stream ->
//                                            stream.toExternalResource().use { resource ->
//                                                contact.uploadAudio(resource).also {
//                                                    add(it)
//                                                }
//                                            }
//                                        }


                                        inputStream.use { stream ->
                                            stream.toExternalResource().use { resource ->
                                                contact.uploadAudio(resource).also {
                                                    add(it)
                                                }
                                            }
                                        }
                                        inputPFD.close()
                                    }
                                }
                                is com.ojhdtapp.messagedto.message_content.QuoteReply -> {
                                    it.quoteMessageId?.also {
                                        repository.getMiraiMessageById(it)?.let {
                                            add(
//                                                MiraiCode.deserializeMiraiCode(it.miraiCode).quote()
                                                MessageChain.deserializeFromJsonString(it.json)
                                                    .quote()
                                            )
                                        }
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                    val receipt = contact.sendMessage(messageChain)
                    if (messageId != null) {
                        receiptMap.put(messageId, receipt)
                        sendMessageSendStateToMainApp(messageId, true)
                    }
                }
            } catch (e: NoSuchElementException) {
                e.printStackTrace()
                if (messageId != null) {
                    sendMessageSendStateToMainApp(messageId, false)
                }
            } catch (e: EventCancelledException) {
                e.printStackTrace()
                if (messageId != null) {
                    sendMessageSendStateToMainApp(messageId, false)
                }
            } catch (e: BotIsBeingMutedException) {
                e.printStackTrace()
                if (messageId != null) {
                    sendMessageSendStateToMainApp(messageId, false)
                }
            } catch (e: MessageTooLargeException) {
                e.printStackTrace()
                if (messageId != null) {
                    sendMessageSendStateToMainApp(messageId, false)
                }
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                if (messageId != null) {
                    sendMessageSendStateToMainApp(messageId, false)
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                if (messageId != null) {
                    sendMessageSendStateToMainApp(messageId, false)
                }
            } catch (e: ServiceConfigurationError) {
                if (messageId != null) {
                    sendMessageSendStateToMainApp(messageId, false)
                }
                e.printStackTrace()
            } catch (e: IOException) {
                if (messageId != null) {
                    sendMessageSendStateToMainApp(messageId, false)
                }
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun recallMessage(messageId: Long) {
        try {
            lifecycleScope.launch() {
                withTimeout(1000) {
                    val receipt = receiptMap[messageId]
                    if (receipt == null) {
                        throw (NoSuchElementException("no receipt"))
                    } else {
                        receipt.recall()
                        sendMessageRecallStateToMainApp(true, messageId)
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            sendMessageRecallStateToMainApp(false, messageId)
        } catch (e: NoSuchElementException) {
            sendMessageRecallStateToMainApp(false, messageId)
        } catch (e: IllegalStateException) {
            sendMessageRecallStateToMainApp(false, messageId)
        }
    }

    override fun onCreate() {
        sMessenger = Messenger(ConnHandler())
        notificationUtil = NotificationUtilForService(this)
        connectionType = packageManager.getApplicationInfo(
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
                        ConnKey.MSG_MESSAGE_SEND -> {
//                            val messageId = (msg.obj as Bundle).getLong("message_id")
                            msg.data.classLoader = SendMessageDto::class.java.classLoader
                            msg.data.getParcelable<SendMessageDto>("value")?.let {
                                Log.d("parabox", "transfer success! value: $it")
                                sendMessage(it)
                            }
                        }
                        ConnKey.MSG_MESSAGE_RECALL -> {
                            (msg.obj as Bundle).getLong("message_id").also {
                                recallMessage(it)
                            }
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
            val deviceInfo = withContext(Dispatchers.IO) {
                repository.getDeviceInfo()
            } ?: DeviceInfo.random()
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