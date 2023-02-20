package com.ojhdtapp.miraipluginforparabox.domain.service

import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.*
import android.os.Message
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import com.ojhdtapp.miraipluginforparabox.R
import com.ojhdtapp.miraipluginforparabox.core.MIRAI_CORE_VERSION
import com.ojhdtapp.miraipluginforparabox.core.util.DataStoreKeys
import com.ojhdtapp.miraipluginforparabox.core.util.NotificationUtilForService
import com.ojhdtapp.miraipluginforparabox.core.util.dataStore
import com.ojhdtapp.miraipluginforparabox.core.util.getTimestampInSecond
import com.ojhdtapp.miraipluginforparabox.data.local.entity.MiraiMessageEntity
import com.ojhdtapp.miraipluginforparabox.data.remote.api.FileDownloadService
import com.ojhdtapp.miraipluginforparabox.domain.repository.MainRepository
import com.ojhdtapp.miraipluginforparabox.domain.util.*
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxKey
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxMetadata
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxService
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import moe.ore.silk.AudioUtils
import net.mamoe.mirai.Bot
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.contact.roaming.RoamingMessageFilter
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.internal.deps.io.ktor.util.collections.CopyOnWriteHashMap
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.MessageChain.Companion.serializeToJsonString
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.network.LoginFailedException
import net.mamoe.mirai.utils.BotConfiguration
import net.mamoe.mirai.utils.DeviceVerificationRequests
import net.mamoe.mirai.utils.DeviceVerificationResult
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.LoginSolver
import net.mamoe.mirai.utils.MiraiInternalApi
import net.mamoe.mirai.utils.ProgressionCallback.Companion.asProgressionCallback
import xyz.cssxsh.mirai.device.MiraiDeviceGenerator
import java.io.*
import java.util.*
import javax.inject.Inject
import kotlin.NoSuchElementException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration.Companion.seconds
import java.util.concurrent.ConcurrentHashMap

@AndroidEntryPoint
class ConnService : ParaboxService() {
    @Inject
    lateinit var repository: MainRepository

    @Inject
    lateinit var downloadService: FileDownloadService
    lateinit var notificationUtil: NotificationUtilForService
    private var bot: Bot? = null
    private var receiptMap = mutableMapOf<Long, MessageReceipt<Contact>>()
    var gettingRoamingMessage = false

    companion object {
        var connectionType = 0

        // request code
        const val REQUEST_SOLVE_PIC_CAPTCHA = 30
        const val REQUEST_SOLVE_SLIDER_CAPTCHA = 31
        const val REQUEST_SOLVE_UNSAFE_DEVICE_LOGIN_VERIFY = 32
        const val REQUEST_SOLVE_DEVICE_VERIFICATION_SMS = 33
        const val REQUEST_SOLVE_DEVICE_VERIFICATION_FALLBACK = 34

        init {
            System.loadLibrary("silkcodec")
        }
    }

    private suspend fun updateLastSuccessfulHandleTimestamp() {
        lifecycleScope.launch(Dispatchers.IO) {
            val currentValue =
                dataStore.data.first()[DataStoreKeys.LAST_SUCCESSFUL_HANDLE_TIMESTAMP]
            val currentTime = System.currentTimeMillis()
            if (currentValue != null && currentTime - currentValue < 1000 * 60 * 3) {
                dataStore.edit { settings ->
                    settings[DataStoreKeys.LAST_SUCCESSFUL_HANDLE_TIMESTAMP] = currentTime
                }
            }
        }
    }

    @OptIn(MiraiInternalApi::class)
    private fun registerEventListener() {
        GlobalEventChannel.parentScope(lifecycleScope).subscribeAlways<MessageEvent> { event ->
            try {
                when (event) {
                    is GroupMessageEvent -> {
                        val messageContents = event.message.toMessageContentList(
                            context = this@ConnService,
                            downloadService = downloadService,
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
                            avatar = event.sender.avatarUrl,
                            id = event.sender.id,
                            avatarUri = null,
                        )
                        val groupProfile = Profile(
                            name = event.group.name,
                            avatar = event.group.avatarUrl,
                            id = event.subject.id,
                            avatarUri = null,
                        )
                        val pluginConnection = PluginConnection(
                            connectionType = connectionType,
                            sendTargetType = SendTargetType.GROUP,
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
                        receiveMessage(dto) {
                            if (it is ParaboxResult.Success) {
                                lifecycleScope.launch {
                                    updateLastSuccessfulHandleTimestamp()
                                }
                            }
                        }
                    }

                    is FriendMessageEvent -> {
                        val messageContents =
                            event.message.toMessageContentList(
                                context = this@ConnService,
                                downloadService = downloadService,
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
                        val profile = Profile(
                            name = event.senderName,
                            avatar = event.sender.avatarUrl,
                            id = event.subject.id,
                            avatarUri = null,
                        )
                        val pluginConnection = PluginConnection(
                            connectionType = connectionType,
                            sendTargetType = SendTargetType.USER,
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
                        receiveMessage(dto) {
                            if (it is ParaboxResult.Success) {
                                lifecycleScope.launch {
                                    updateLastSuccessfulHandleTimestamp()
                                }
                            }
                        }
                    }

                    is GroupMessageSyncEvent -> {
                        val messageContents =
                            event.message.toMessageContentList(
                                context = this@ConnService,
                                downloadService = downloadService,
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
                        val pluginConnection = PluginConnection(
                            connectionType = connectionType,
                            sendTargetType = SendTargetType.GROUP,
                            id = event.subject.id
                        )
                        val dto = SendMessageDto(
                            contents = messageContents,
                            timestamp = "${event.time}000".toLong(),
                            pluginConnection = pluginConnection,
                            messageId = messageId,
                        )
                        syncMessage(dto) {
                            if (it is ParaboxResult.Success) {
                                lifecycleScope.launch {
                                    updateLastSuccessfulHandleTimestamp()
                                }
                            }
                        }
                    }

                    is FriendMessageSyncEvent -> {
                        val messageContents =
                            event.message.toMessageContentList(
                                context = this@ConnService,
                                downloadService = downloadService,
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
                        val pluginConnection = PluginConnection(
                            connectionType = connectionType,
                            sendTargetType = SendTargetType.USER,
                            id = event.subject.id
                        )
                        val dto = SendMessageDto(
                            contents = messageContents,
                            timestamp = "${event.time}000".toLong(),
                            pluginConnection = pluginConnection,
                            messageId = messageId,
                        )
                        syncMessage(dto) {
                            if (it is ParaboxResult.Success) {
                                lifecycleScope.launch {
                                    updateLastSuccessfulHandleTimestamp()
                                }
                            }
                        }
                    }

                    else -> {
                        Log.d("parabox", event.toString())
                        val messageContents =
                            event.message.toMessageContentList(
                                context = this@ConnService,
                                downloadService = downloadService,
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
                        val profile = Profile(
                            name = event.senderName,
                            avatar = event.sender.avatarUrl,
                            id = event.subject.id,
                            avatarUri = null,
                        )
                        val pluginConnection = PluginConnection(
                            connectionType = connectionType,
                            sendTargetType = SendTargetType.USER,
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
                        receiveMessage(dto) {
                            if (it is ParaboxResult.Success) {
                                lifecycleScope.launch {
                                    updateLastSuccessfulHandleTimestamp()
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        GlobalEventChannel.parentScope(lifecycleScope).subscribeAlways<BotOnlineEvent> {
            updateServiceState(ParaboxKey.STATE_RUNNING, "Mirai Core - $MIRAI_CORE_VERSION")
        }

        GlobalEventChannel.parentScope(lifecycleScope).subscribeAlways<BotOfflineEvent> {
            when (it) {
                is BotOfflineEvent.Active -> {
                    updateServiceState(
                        ParaboxKey.STATE_STOP,
                        getString(R.string.account_offline_initiative)
                    )
                    notificationUtil.updateForegroundServiceNotification(
                        getString(R.string.account_offline_initiative)
                    )
                }

                is BotOfflineEvent.Force -> {
                    updateServiceState(
                        ParaboxKey.STATE_ERROR,
                        getString(R.string.account_offline_signin_else_where)
                    )
                    notificationUtil.updateForegroundServiceNotification(
                        getString(R.string.account_offline_signin_else_where)
                    )
                }

                is BotOfflineEvent.Dropped -> {
                    updateServiceState(
                        ParaboxKey.STATE_LOADING,
                        getString(R.string.account_offline_poor_network)
                    )
                    notificationUtil.updateForegroundServiceNotification(
                        getString(R.string.account_offline_poor_network)
                    )
                }

                is BotOfflineEvent.RequireReconnect -> {
                    updateServiceState(
                        ParaboxKey.STATE_LOADING,
                        getString(R.string.account_offline_server_changed)
                    )
                    notificationUtil.updateForegroundServiceNotification(
                        getString(R.string.account_offline_server_changed)
                    )
                }

                else -> {
                    updateServiceState(
                        ParaboxKey.STATE_ERROR,
                        getString(R.string.account_offline_unknown)
                    )
                    notificationUtil.updateForegroundServiceNotification(
                        getString(R.string.account_offline_unknown)
                    )
                }
            }
        }
        GlobalEventChannel.parentScope(lifecycleScope).subscribeAlways<BotReloginEvent> {
            updateServiceState(ParaboxKey.STATE_RUNNING, "Mirai Core - $MIRAI_CORE_VERSION")
            notificationUtil.updateForegroundServiceNotification(getString(R.string.mirai_running))
        }
    }

    private fun getRoamingMessages() {
        if (bot != null && !gettingRoamingMessage) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    gettingRoamingMessage = true
                    val currentTime = System.currentTimeMillis()
                    val lastSuccessfulHandleTimestamp =
                        dataStore.data.first()[DataStoreKeys.LAST_SUCCESSFUL_HANDLE_TIMESTAMP] ?: 0
                    Log.d(
                        "RoamingMessage",
                        "lastSuccessfulHandleTimestamp: ${lastSuccessfulHandleTimestamp.getTimestampInSecond()}"
                    )
//                    val jobMap = mutableMapOf<String, Job>()
                    val jobMap = ConcurrentHashMap<String, Job>()
//                    val timestampMap = mutableMapOf<String, Long>()
                    val timestampMap = ConcurrentHashMap<String, Long>()
                    bot!!.groups.forEach { group ->
                        launch {
                            try {
                                group.roamingMessages.getMessagesIn(
                                    timeStart = lastSuccessfulHandleTimestamp.getTimestampInSecond(),
                                    timeEnd = currentTime.getTimestampInSecond(),
                                    filter = RoamingMessageFilter.RECEIVED
                                ).collect {
                                    timestampMap["${group.id}gr"] = System.currentTimeMillis()
                                    handleGroupRoamingReceiveMessage(it, group)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }.also { jobMap["${group.id}gr"] = it }
                        launch {
                            group.roamingMessages.getMessagesIn(
                                timeStart = lastSuccessfulHandleTimestamp.getTimestampInSecond(),
                                timeEnd = currentTime.getTimestampInSecond(),
                                filter = RoamingMessageFilter.SENT
                            ).collect {
                                timestampMap["${group.id}gs"] = System.currentTimeMillis()
                                handleGroupRoamingSendMessage(it, group)
                            }
                        }.also { jobMap["${group.id}gs"] = it }
                    }
                    bot!!.friends.forEach { friend ->
                        launch {
                            friend.roamingMessages.getMessagesIn(
                                timeStart = lastSuccessfulHandleTimestamp.getTimestampInSecond(),
                                timeEnd = currentTime.getTimestampInSecond(),
                                filter = RoamingMessageFilter.RECEIVED
                            ).collect {
                                timestampMap["${friend.id}fr"] = System.currentTimeMillis()
                                handleFriendRoamingReceiveMessage(it, friend)
                            }
                        }.also { jobMap["${friend.id}fr"] = it }
                        launch {
                            friend.roamingMessages.getMessagesIn(
                                timeStart = lastSuccessfulHandleTimestamp.getTimestampInSecond(),
                                timeEnd = currentTime.getTimestampInSecond(),
                                filter = RoamingMessageFilter.SENT
                            ).collect {
                                timestampMap["${friend.id}fs"] = System.currentTimeMillis()
                                handleFriendRoamingSendMessage(it, friend)
                            }
                        }.also { jobMap["${friend.id}fs"] = it }
                    }
                    launch {
                        while (jobMap.values.any { it.isActive } && System.currentTimeMillis() - currentTime < 16000) {
                            delay(1000)
                            timestampMap.forEach { id, timestamp ->
                                if (System.currentTimeMillis() - timestamp > 2000) {
                                    jobMap[id]?.cancel()
                                    jobMap.remove(id)
                                    timestampMap.remove(id)
                                }
                            }
                        }
                        Log.d(
                            "Roaming",
                            "Roaming message collection finished:${System.currentTimeMillis()}"
                        )
                        gettingRoamingMessage = false
                        jobMap.values.forEach { it.cancel() }
                        jobMap.clear()
                        timestampMap.clear()
                        dataStore.edit { settings ->
                            settings[DataStoreKeys.LAST_SUCCESSFUL_HANDLE_TIMESTAMP] = currentTime
                        }
                    }
                } catch (e: Exception) {
                    gettingRoamingMessage = false
                    e.printStackTrace()
                }
            }
        }
    }

    private suspend fun handleGroupRoamingReceiveMessage(message: MessageChain, group: Group) {
        val sender = group.get(message.source.fromId)
            ?: bot!!.getStranger(message.source.fromId)

        val messageContents =
            message.toMessageContentList(
                context = this@ConnService,
                downloadService = downloadService,
                bot = message.bot,
                fromRoaming = false,
                repository = repository
            )
        val messageId = getMessageId(message.ids)
        messageId?.let {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    repository.insertMiraiMessage(
                        MiraiMessageEntity(
                            it,
                            message.serializeToMiraiCode(),
                            message.serializeToJsonString()
                        )
                    )

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        val profile = Profile(
            name = sender?.remarkOrNick ?: "Unknown",
            avatar = sender?.avatarUrl,
            id = message.source.fromId,
            avatarUri = null,
        )
        val subjectProfile = Profile(
            name = group.name,
            avatar = group.avatarUrl,
            id = group.id,
            avatarUri = null,
        )
        val pluginConnection = PluginConnection(
            connectionType = connectionType,
            sendTargetType = SendTargetType.GROUP,
            id = group.id
        )
        val dto = ReceiveMessageDto(
            contents = messageContents,
            profile = profile,
            subjectProfile = subjectProfile,
            timestamp = "${message.time}000".toLong(),
            messageId = messageId,
            pluginConnection = pluginConnection
        )
        receiveMessage(dto) {
            if (it is ParaboxResult.Success) {

            }
        }
    }

    private suspend fun handleGroupRoamingSendMessage(message: MessageChain, group: Group) {
        val messageContents =
            message.toMessageContentList(
                context = this@ConnService,
                downloadService = downloadService,
                bot = message.bot,
                fromRoaming = false,
                repository = repository
            )
        val messageId = getMessageId(message.ids)

        messageId?.let {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    repository.insertMiraiMessage(
                        MiraiMessageEntity(
                            it,
                            message.serializeToMiraiCode(),
                            message.serializeToJsonString()
                        )
                    )

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        val pluginConnection = PluginConnection(
            connectionType = connectionType,
            sendTargetType = SendTargetType.GROUP,
            id = group.id
        )
        val dto = SendMessageDto(
            contents = messageContents,
            timestamp = "${message.time}000".toLong(),
            messageId = messageId,
            pluginConnection = pluginConnection
        )
        syncMessage(dto) {
            if (it is ParaboxResult.Success) {

            }
        }
    }

    private suspend fun handleFriendRoamingReceiveMessage(message: MessageChain, friend: Friend) {
        val messageContents =
            message.toMessageContentList(
                context = this@ConnService,
                downloadService = downloadService,
                bot = message.bot,
                fromRoaming = false,
                repository = repository
            )
        val messageId = getMessageId(message.ids)
        messageId?.let {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    repository.insertMiraiMessage(
                        MiraiMessageEntity(
                            it,
                            message.serializeToMiraiCode(),
                            message.serializeToJsonString()
                        )
                    )

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        val profile = Profile(
            name = friend.remarkOrNick,
            avatar = friend.avatarUrl,
            id = friend.id,
            avatarUri = null,
        )
        val pluginConnection = PluginConnection(
            connectionType = connectionType,
            sendTargetType = SendTargetType.USER,
            id = friend.id
        )
        val dto = ReceiveMessageDto(
            contents = messageContents,
            profile = profile,
            subjectProfile = profile,
            timestamp = "${message.time}000".toLong(),
            messageId = messageId,
            pluginConnection = pluginConnection
        )
        receiveMessage(dto) {
            if (it is ParaboxResult.Success) {

            }
        }
    }

    private suspend fun handleFriendRoamingSendMessage(message: MessageChain, friend: Friend) {
        val messageContents =
            message.toMessageContentList(
                context = this@ConnService,
                downloadService = downloadService,
                bot = message.bot,
                fromRoaming = false,
                repository = repository
            )
        val messageId = getMessageId(message.ids)
        messageId?.let {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    repository.insertMiraiMessage(
                        MiraiMessageEntity(
                            it,
                            message.serializeToMiraiCode(),
                            message.serializeToJsonString()
                        )
                    )

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        val pluginConnection = PluginConnection(
            connectionType = connectionType,
            sendTargetType = SendTargetType.USER,
            id = friend.id
        )
        val dto = SendMessageDto(
            contents = messageContents,
            timestamp = "${message.time}000".toLong(),
            messageId = messageId,
            pluginConnection = pluginConnection
        )
        syncMessage(dto) {
            if (it is ParaboxResult.Success) {

            }
        }
    }

    override fun onStartParabox() {
        updateServiceState(ParaboxKey.STATE_LOADING, getString(R.string.authentication_running))
        lifecycleScope.launch {
            try {
                val secret = withContext(Dispatchers.IO) {
                    repository.getSelectedAccount()
                }
                val isForegroundServiceEnabled =
                    dataStore.data.first()[DataStoreKeys.FOREGROUND_SERVICE] ?: true
                if (isForegroundServiceEnabled) {
                    notificationUtil.startForegroundService()
                }
//                val mDeviceInfo = withContext(Dispatchers.IO) {
//                    repository.getDeviceInfo()
//                } ?: DeviceInfo.random()
                val mLoginSolver = AndroidLoginSolver()
                val isContactCacheEnabled =
                    dataStore.data.first()[DataStoreKeys.CONTACT_CACHE] ?: false
                val selectedProtocol =
                    when (dataStore.data.first()[DataStoreKeys.PROTOCOL] ?: MiraiProtocol.Pad) {
                        MiraiProtocol.Phone -> BotConfiguration.MiraiProtocol.ANDROID_PHONE
                        MiraiProtocol.Pad -> BotConfiguration.MiraiProtocol.ANDROID_PAD
                        MiraiProtocol.Watch -> BotConfiguration.MiraiProtocol.ANDROID_WATCH
                        MiraiProtocol.IPad -> BotConfiguration.MiraiProtocol.IPAD
                        MiraiProtocol.MacOS -> BotConfiguration.MiraiProtocol.MACOS
                        else -> BotConfiguration.MiraiProtocol.ANDROID_PAD
                    }
                if (secret == null) {
                    updateServiceState(
                        ParaboxKey.STATE_ERROR,
                        getString(R.string.account_not_selected)
                    )
                    return@launch
                }
                bot = BotFactory.newBot(secret.account, secret.password) {
                    workingDir = getExternalFilesDir("working")!!.absoluteFile
                    loginSolver = mLoginSolver
                    cacheDir = getExternalFilesDir("cache")!!.absoluteFile
                    protocol = selectedProtocol
                    if (isContactCacheEnabled) enableContactCache()
                    deviceInfo = MiraiDeviceGenerator()::load
                }.also {
                    registerEventListener()
                    it.login()
                    updateServiceState(ParaboxKey.STATE_RUNNING, "Mirai Core - $MIRAI_CORE_VERSION")
//                    notificationUtil.updateForegroundServiceNotification(
//                        getString(R.string.service_running_normally),
//                        "Mirai Core - $version"
//                    )
                }
            } catch (e: IOException) {
                updateServiceState(ParaboxKey.STATE_ERROR, getString(R.string.error_io))
                e.printStackTrace()
            } catch (e: NoSuchElementException) {
                updateServiceState(ParaboxKey.STATE_ERROR, getString(R.string.error_data_lost))
                e.printStackTrace()
            } catch (e: LoginFailedException) {
                updateServiceState(ParaboxKey.STATE_ERROR, getString(R.string.error_login_failed))
                e.printStackTrace()
            } catch (e: Exception) {
                updateServiceState(ParaboxKey.STATE_ERROR, getString(R.string.error_unknown))
                e.printStackTrace()
            }
        }
    }

    override fun onStopParabox() {
        bot?.close()
        bot = null
        notificationUtil.stopForegroundService()
        updateServiceState(ParaboxKey.STATE_STOP)
    }

    override fun onStateUpdate(state: Int, message: String?) {
        Log.d("parabox", "state update: $state")
    }

    override fun customHandleMessage(msg: Message, metadata: ParaboxMetadata) {

    }

    @OptIn(FlowPreview::class)
    override suspend fun onSendMessage(dto: SendMessageDto): Boolean {
        Log.d("parabox", dto.toString())
        val messageId = dto.messageId
        return withContext(Dispatchers.IO) {
            try {
                val targetId = dto.pluginConnection.id
                val targetContact = when (dto.pluginConnection.sendTargetType) {
                    SendTargetType.USER -> bot?.getFriend(targetId)
                    SendTargetType.GROUP -> bot?.getGroup(targetId)
                    else -> {
                        throw java.util.NoSuchElementException("wrong id")
                    }
                }
                targetContact?.let { contact ->
                    val contents = dto.contents
                    val messageChain = buildMessageChain {
                        contents.map {
                            when (it) {
                                is com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.PlainText -> add(
                                    PlainText(it.text)
                                )

                                is com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.Image -> {
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

                                is com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.At -> add(
                                    At(it.target)
                                )

                                is com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.AtAll -> add(
                                    AtAll
                                )

                                is com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.Audio -> {
                                    it.uri?.let { uri ->
                                        val inputPFD: ParcelFileDescriptor? =
                                            contentResolver.openFileDescriptor(uri, "r")
                                        val fd = inputPFD!!.fileDescriptor
                                        val inputStream = FileInputStream(fd)
                                        AudioUtils.init(this@ConnService.externalCacheDir!!.absolutePath)
                                        AudioUtils.mp3ToSilk(inputStream).inputStream()
                                            .use { silk ->
                                                silk.toExternalResource().use { resource ->
                                                    contact.uploadAudio(resource).also {
                                                        add(it)
                                                        inputPFD.close()
                                                    }
                                                }
                                            }

                                    }
                                }

                                is com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.QuoteReply -> {
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
                                is com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.File -> {
                                    if (dto.pluginConnection.sendTargetType == SendTargetType.GROUP) {
                                        it.uri?.let { uri ->
                                            val inputPFD: ParcelFileDescriptor? =
                                                contentResolver.openFileDescriptor(uri, "r")
                                            val fd = inputPFD!!.fileDescriptor
                                            val inputStream = FileInputStream(fd)
                                            inputStream.use { stream ->
                                                stream.toExternalResource().use { resource ->
                                                    val progress = Channel<Long>(Channel.BUFFERED)
                                                    launch {
                                                        progress.receiveAsFlow().sample(1.seconds)
                                                            .collect { bytes ->
                                                                val progress =
                                                                    (bytes.toDouble() / resource.size * 100).toInt() / 100
                                                            }
                                                    }
                                                    (contact as Group).files.uploadNewFile(
                                                        "/${it.name}",
                                                        resource,
                                                        progress.asProgressionCallback(true)
                                                    )
                                                }
                                            }
                                            inputPFD.close()
                                        }
                                    } else {

                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                    if (!messageChain.isContentEmpty()) {
                        val receipt = contact.sendMessage(messageChain)
                        if (messageId != null) {
                            receiptMap[messageId] = receipt
                        }
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    override suspend fun onRecallMessage(messageId: Long): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                withTimeout(1000) {
                    val receipt = receiptMap[messageId]
                    if (receipt == null) {
                        throw (java.util.NoSuchElementException("no receipt"))
                    } else {
                        receipt.recall()
                    }
                }
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    override fun onRefreshMessage() {
        getRoamingMessages()
    }

    override fun onMainAppLaunch() {
        Log.d("parabox", "main app launched")
        if (getServiceState() == ParaboxKey.STATE_STOP) {
            lifecycleScope.launch {
                val isAutoLoginEnabled =
                    dataStore.data.first()[DataStoreKeys.AUTO_LOGIN] ?: false
                if (isAutoLoginEnabled) {
                    onStartParabox()
                }
            }
        }
    }


    inner class AndroidLoginSolver() : LoginSolver() {
        override suspend fun onSolvePicCaptcha(bot: Bot, data: ByteArray): String? {
            Log.d("parabox", "onSolvePicCaptcha")
            return suspendCoroutine<String?> { cot ->
                updateServiceState(
                    ParaboxKey.STATE_PAUSE,
                    getString(R.string.authentication_notice)
                )
                val bm = BitmapFactory.decodeByteArray(data, 0, data.size)
                sendRequest(
                    request = ConnService.REQUEST_SOLVE_PIC_CAPTCHA,
                    client = ParaboxKey.CLIENT_CONTROLLER,
                    extra = Bundle().apply {
                        putParcelable("bitmap", bm)
                    },
                    timeoutMillis = 60000,
                    onResult = {
                        if (it is ParaboxResult.Success) {
                            updateServiceState(
                                ParaboxKey.STATE_LOADING,
                                getString(R.string.try_to_login)
                            )
                            it.obj.getString("result").also {
                                cot.resume(it)
                            }
                        } else {
                            val errMessage = when ((it as ParaboxResult.Fail).errorCode) {
                                ParaboxKey.ERROR_RESOURCE_NOT_FOUND -> getString(R.string.error_resource_not_found)
                                ParaboxKey.ERROR_DISCONNECTED -> getString(R.string.error_disconnected)
                                ParaboxKey.ERROR_REPEATED_CALL -> getString(R.string.error_repeated_call)
                                ParaboxKey.ERROR_TIMEOUT -> getString(R.string.error_timeout)
                                else -> getString(R.string.error_unknown)
                            }
                            cot.resume(null)
                            updateServiceState(ParaboxKey.STATE_ERROR, errMessage)
                        }
                    }
                )
            }
        }

        override suspend fun onSolveSliderCaptcha(bot: Bot, url: String): String? {
            Log.d("parabox", "onSolveSliderCaptcha")
            return suspendCoroutine<String?> { cot ->
                updateServiceState(
                    ParaboxKey.STATE_PAUSE,
                    getString(R.string.authentication_notice)
                )
                sendRequest(
                    request = ConnService.REQUEST_SOLVE_SLIDER_CAPTCHA,
                    client = ParaboxKey.CLIENT_CONTROLLER,
                    extra = Bundle().apply {
                        putString("url", url)
                    },
                    timeoutMillis = 90000,
                    onResult = {
                        if (it is ParaboxResult.Success) {
                            updateServiceState(
                                ParaboxKey.STATE_LOADING,
                                getString(R.string.try_to_login)
                            )
                            it.obj.getString("result").also {
                                cot.resume(it)
                            }
                        } else {
                            val errMessage = when ((it as ParaboxResult.Fail).errorCode) {
                                ParaboxKey.ERROR_RESOURCE_NOT_FOUND -> getString(R.string.error_resource_not_found)
                                ParaboxKey.ERROR_DISCONNECTED -> getString(R.string.error_disconnected)
                                ParaboxKey.ERROR_REPEATED_CALL -> getString(R.string.error_repeated_call)
                                ParaboxKey.ERROR_TIMEOUT -> getString(R.string.error_timeout)
                                else -> getString(R.string.error_unknown)
                            }
                            cot.resume(null)
                            updateServiceState(ParaboxKey.STATE_ERROR, errMessage)
                        }
                    }
                )
            }
        }

        override suspend fun onSolveDeviceVerification(
            bot: Bot,
            requests: DeviceVerificationRequests
        ): DeviceVerificationResult {
            Log.d("parabox", "onSolveDeviceVerification")
            return if (requests.sms != null) {
                updateServiceState(
                    ParaboxKey.STATE_PAUSE,
                    getString(R.string.sms_authentication_notice)
                )
                requests.sms!!.let {
                    it.requestSms()
                    it.solved(
                        suspendCoroutine<String> { cot ->
                            sendRequest(
                                request = ConnService.REQUEST_SOLVE_DEVICE_VERIFICATION_SMS,
                                client = ParaboxKey.CLIENT_CONTROLLER,
                                extra = Bundle().apply {
                                    putString("number", it.phoneNumber)
                                },
                                timeoutMillis = 90000,
                                onResult = {
                                    if (it is ParaboxResult.Success) {
                                        updateServiceState(
                                            ParaboxKey.STATE_LOADING,
                                            getString(R.string.try_to_login)
                                        )
                                        it.obj.getString("result").also {
                                            it?.let { cot.resume(it) } ?: cot.resumeWithException(
                                                NoSuchElementException("missing result")
                                            )
                                        }
                                    } else {
                                        val errMessage =
                                            when ((it as ParaboxResult.Fail).errorCode) {
                                                ParaboxKey.ERROR_RESOURCE_NOT_FOUND -> getString(R.string.error_resource_not_found)
                                                ParaboxKey.ERROR_DISCONNECTED -> getString(R.string.error_disconnected)
                                                ParaboxKey.ERROR_REPEATED_CALL -> getString(R.string.error_repeated_call)
                                                ParaboxKey.ERROR_TIMEOUT -> getString(R.string.error_timeout)
                                                else -> getString(R.string.error_unknown)
                                            }
                                        cot.resumeWithException(IOException("connection failed"))
                                        updateServiceState(ParaboxKey.STATE_ERROR, errMessage)
                                    }
                                }
                            )
                        }
                    )
                }
            } else {
                requests.fallback!!.let {
                    updateServiceState(
                        ParaboxKey.STATE_PAUSE,
                        getString(R.string.sms_authentication_notice)
                    )
                    val res = suspendCoroutine<Boolean> { cot ->
                        sendRequest(
                            request = ConnService.REQUEST_SOLVE_DEVICE_VERIFICATION_FALLBACK,
                            client = ParaboxKey.CLIENT_CONTROLLER,
                            extra = Bundle().apply {
                                putString("url", it.url)
                            },
                            timeoutMillis = 90000,
                            onResult = {
                                if (it is ParaboxResult.Success) {
                                    updateServiceState(
                                        ParaboxKey.STATE_LOADING,
                                        getString(R.string.try_to_login)
                                    )
                                    cot.resume(true)
                                } else {
                                    val errMessage =
                                        when ((it as ParaboxResult.Fail).errorCode) {
                                            ParaboxKey.ERROR_RESOURCE_NOT_FOUND -> getString(R.string.error_resource_not_found)
                                            ParaboxKey.ERROR_DISCONNECTED -> getString(R.string.error_disconnected)
                                            ParaboxKey.ERROR_REPEATED_CALL -> getString(R.string.error_repeated_call)
                                            ParaboxKey.ERROR_TIMEOUT -> getString(R.string.error_timeout)
                                            else -> getString(R.string.error_unknown)
                                        }
                                    cot.resume(false)
                                    updateServiceState(ParaboxKey.STATE_ERROR, errMessage)
                                }
                            }
                        )
                    }
                    if (res) {
                        it.solved()
                    } else {
                        throw (Exception("failed"))
                    }
                }
            }
        }

//        @Deprecated(
//            "Please use onSolveDeviceVerification instead",
//            replaceWith = ReplaceWith("onSolveDeviceVerification(bot, url, null)"),
//            level = DeprecationLevel.WARNING
//        )
//        override suspend fun onSolveUnsafeDeviceLoginVerify(bot: Bot, url: String): String? {
//            updateServiceState(ParaboxKey.STATE_PAUSE, getString(R.string.sms_authentication_notice))
//            val deferred = CompletableDeferred<String>()
//            sendRequest(
//                request = ConnService.REQUEST_SOLVE_UNSAFE_DEVICE_LOGIN_VERIFY,
//                client = ParaboxKey.CLIENT_CONTROLLER,
//                extra = Bundle().apply {
//                    putString("url", url)
//                },
//                timeoutMillis = 300000,
//                onResult = {
//                    if (it is ParaboxResult.Success) {
//                        updateServiceState(ParaboxKey.STATE_LOADING, getString(R.string.try_to_login))
//                        it.obj.getString("result").also {
//                            if (it != null) deferred.complete(it)
//                            else throw NoSuchElementException("result not found")
//                        }
//                    } else {
//                        val errMessage = when ((it as ParaboxResult.Fail).errorCode) {
//                            ParaboxKey.ERROR_RESOURCE_NOT_FOUND -> getString(R.string.error_resource_not_found)
//                            ParaboxKey.ERROR_DISCONNECTED -> getString(R.string.error_disconnected)
//                            ParaboxKey.ERROR_REPEATED_CALL -> getString(R.string.error_repeated_call)
//                            ParaboxKey.ERROR_TIMEOUT -> getString(R.string.error_timeout)
//                            else -> getString(R.string.error_unknown)
//                        }
//                        deferred.completeExceptionally(IOException("result transmission failed"))
//                        updateServiceState(ParaboxKey.STATE_ERROR, errMessage)
//                    }
//                }
//            )
//            return deferred.await()
//        }

        override val isSliderCaptchaSupported: Boolean
            get() = true
    }

    override fun onCreate() {
        notificationUtil = NotificationUtilForService(this)
        connectionType = packageManager.getApplicationInfo(
            this@ConnService.packageName,
            PackageManager.GET_META_DATA
        ).metaData.getInt("connection_type")
        super.onCreate()
    }
}