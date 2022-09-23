package com.ojhdtapp.miraipluginforparabox.domain.service

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.*
import android.os.Message
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.ojhdtapp.messagedto.*
import com.ojhdtapp.miraipluginforparabox.core.MIRAI_CORE_VERSION
import com.ojhdtapp.miraipluginforparabox.core.util.DataStoreKeys
import com.ojhdtapp.miraipluginforparabox.core.util.NotificationUtilForService
import com.ojhdtapp.miraipluginforparabox.core.util.dataStore
import com.ojhdtapp.miraipluginforparabox.data.local.entity.MiraiMessageEntity
import com.ojhdtapp.miraipluginforparabox.data.remote.api.FileDownloadService
import com.ojhdtapp.miraipluginforparabox.domain.repository.MainRepository
import com.ojhdtapp.miraipluginforparabox.domain.util.*
import com.ojhdtapp.miraipluginforparabox.toolkit.ParaboxKey
import com.ojhdtapp.miraipluginforparabox.toolkit.ParaboxMetadata
import com.ojhdtapp.miraipluginforparabox.toolkit.ParaboxResult
import com.ojhdtapp.miraipluginforparabox.toolkit.ParaboxService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import moe.ore.silk.AudioUtils
import net.mamoe.mirai.Bot
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.contact.BotIsBeingMutedException
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.MessageTooLargeException
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.Listener
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.events.BotOfflineEvent
import net.mamoe.mirai.event.events.BotOnlineEvent
import net.mamoe.mirai.event.events.BotReloginEvent
import net.mamoe.mirai.event.events.EventCancelledException
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.MessageChain.Companion.serializeToJsonString
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.network.LoginFailedException
import net.mamoe.mirai.utils.BotConfiguration
import net.mamoe.mirai.utils.DeviceInfo
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.LoginSolver
import net.mamoe.mirai.utils.MiraiInternalApi
import okhttp3.internal.wait
import xyz.cssxsh.mirai.device.MiraiDeviceGenerator
import java.io.*
import java.util.*
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.NoSuchElementException

@AndroidEntryPoint
class ConnService : ParaboxService() {
    @Inject
    lateinit var repository: MainRepository
    lateinit var notificationUtil: NotificationUtilForService
    private var bot: Bot? = null

    companion object {
        var connectionType = 0

        // request code
        const val REQUEST_SOLVE_PIC_CAPTCHA = 30
        const val REQUEST_SOLVE_SLIDER_CAPTCHA = 31
        const val REQUEST_SOLVE_UNSAFE_DEVICE_LOGIN_VERIFY = 32

        init {
            System.loadLibrary("silkcodec")
        }
    }

    @OptIn(MiraiInternalApi::class)
    private fun registerEventListener() {
        GlobalEventChannel.parentScope(lifecycleScope).subscribeAlways<MessageEvent> { event ->
            when (event) {
                is GroupMessageEvent -> {

                }

                is FriendMessageEvent -> {

                }

                else -> {

                }
            }
        }
        GlobalEventChannel.parentScope(lifecycleScope).subscribeAlways<MessageEvent> { event ->
            when (event) {
                is GroupMessageEvent -> {

                }

                is FriendMessageEvent -> {

                }

                else -> {

                }
            }
        }
        GlobalEventChannel.parentScope(lifecycleScope).subscribeAlways<BotOnlineEvent> {
            notificationUtil.updateForegroundServiceNotification(
                "服务正常运行",
                "Mirai Core - $MIRAI_CORE_VERSION"
            )
        }
        GlobalEventChannel.parentScope(lifecycleScope).subscribeAlways<BotOfflineEvent> {
            val message = when (it) {
                is BotOfflineEvent.Active -> "账号主动下线"
                is BotOfflineEvent.Force -> "账号已在他处登陆"
                is BotOfflineEvent.Dropped -> "网络不畅或被服务器断开"
                is BotOfflineEvent.RequireReconnect -> "正在更换连接服务器"
                else -> "账号离线"
            }
            updateServiceState(ParaboxKey.STATE_PAUSE, message)
        }
        GlobalEventChannel.parentScope(lifecycleScope).subscribeAlways<BotReloginEvent> {
            updateServiceState(ParaboxKey.STATE_RUNNING, "Mirai Core - $MIRAI_CORE_VERSION")
            notificationUtil.updateForegroundServiceNotification(
                "服务正常运行",
                "Mirai Core - $MIRAI_CORE_VERSION"
            )
        }
    }

    override fun onStartParabox() {
        updateServiceState(ParaboxKey.STATE_LOADING, "正在进行身份验证")
        lifecycleScope.launch {
            try {
                val secret = withContext(Dispatchers.IO) {
                    repository.getSelectedAccount()
                }
                val isForegroundServiceEnabled =
                    dataStore.data.first()[DataStoreKeys.FOREGROUND_SERVICE] ?: false
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
                    when (dataStore.data.first()[DataStoreKeys.PROTOCOL] ?: MiraiProtocol.Phone) {
                        MiraiProtocol.Phone -> BotConfiguration.MiraiProtocol.ANDROID_PHONE
                        MiraiProtocol.Pad -> BotConfiguration.MiraiProtocol.ANDROID_PAD
                        MiraiProtocol.Watch -> BotConfiguration.MiraiProtocol.ANDROID_WATCH
                        MiraiProtocol.IPad -> BotConfiguration.MiraiProtocol.IPAD
                        MiraiProtocol.MacOS -> BotConfiguration.MiraiProtocol.MACOS
                        else -> BotConfiguration.MiraiProtocol.ANDROID_PHONE
                    }
                if (secret == null) {
                    updateServiceState(ParaboxKey.STATE_ERROR, "未选择登陆账户")
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
                    val version = MIRAI_CORE_VERSION
                    updateServiceState(ParaboxKey.STATE_RUNNING, "Mirai Core - $version")
                    notificationUtil.updateForegroundServiceNotification(
                        "服务正常运行",
                        "Mirai Core - $version"
                    )
                }
            } catch (e: IOException) {
                updateServiceState(ParaboxKey.STATE_ERROR, "数据存取期间发生致命错误")
                e.printStackTrace()
            } catch (e: NoSuchElementException) {
                updateServiceState(ParaboxKey.STATE_ERROR, "必要数据于传输期间丢失")
                e.printStackTrace()
            } catch (e: LoginFailedException) {
                updateServiceState(ParaboxKey.STATE_ERROR, "登陆失败")
                e.printStackTrace()
            } catch (e: IllegalStateException) {
                updateServiceState(ParaboxKey.STATE_ERROR, "未知错误")
                e.printStackTrace()
            }
        }
    }

    override fun onStopParabox() {
        bot?.close()
        bot = null
        updateServiceState(ParaboxKey.STATE_STOP)
        notificationUtil.stopForegroundService()
    }

    override fun onStateUpdate() {
    }

    override fun customHandleMessage(msg: Message, metadata: ParaboxMetadata) {
        TODO("Not yet implemented")
    }

    override fun onSendMessage() {
        TODO("Not yet implemented")
    }

    inner class AndroidLoginSolver() : LoginSolver() {

        override suspend fun onSolvePicCaptcha(bot: Bot, data: ByteArray): String {
            updateServiceState(ParaboxKey.STATE_PAUSE, "请遵照提示完成身份验证")
            val deferred = CompletableDeferred<String>()
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
                        updateServiceState(ParaboxKey.STATE_LOADING, "正在尝试登陆")
                        it.obj.getString("result").also {
                            if (it != null) deferred.complete(it)
                            else throw NoSuchElementException("result not found")
                        }
                    } else {
                        val errMessage = when ((it as ParaboxResult.Fail).errorCode) {
                            ParaboxKey.ERROR_RESOURCE_NOT_FOUND -> "必要资源丢失"
                            ParaboxKey.ERROR_DISCONNECTED -> "与服务的连接断开"
                            ParaboxKey.ERROR_REPEATED_CALL -> "重复正在执行的操作"
                            ParaboxKey.ERROR_TIMEOUT -> "操作超时"
                            else -> "未知错误"
                        }
                        deferred.completeExceptionally(IOException("result transmission failed"))
                        updateServiceState(ParaboxKey.STATE_ERROR, errMessage)
                    }
                }
            )
            return deferred.await()
        }

        override suspend fun onSolveSliderCaptcha(bot: Bot, url: String): String? {
            updateServiceState(ParaboxKey.STATE_PAUSE, "请遵照提示完成身份验证")
            val deferred = CompletableDeferred<String>()
            sendRequest(
                request = ConnService.REQUEST_SOLVE_SLIDER_CAPTCHA,
                client = ParaboxKey.CLIENT_CONTROLLER,
                extra = Bundle().apply {
                    putString("url", url)
                },
                timeoutMillis = 90000,
                onResult = {
                    if (it is ParaboxResult.Success) {
                        updateServiceState(ParaboxKey.STATE_LOADING, "正在尝试登陆")
                        it.obj.getString("result").also {
                            if (it != null) deferred.complete(it)
                            else throw NoSuchElementException("result not found")
                        }
                    } else {
                        val errMessage = when ((it as ParaboxResult.Fail).errorCode) {
                            ParaboxKey.ERROR_RESOURCE_NOT_FOUND -> "必要资源丢失"
                            ParaboxKey.ERROR_DISCONNECTED -> "与服务的连接断开"
                            ParaboxKey.ERROR_REPEATED_CALL -> "重复正在执行的操作"
                            ParaboxKey.ERROR_TIMEOUT -> "操作超时"
                            else -> "未知错误"
                        }
                        deferred.completeExceptionally(IOException("result transmission failed"))
                        updateServiceState(ParaboxKey.STATE_ERROR, errMessage)
                    }
                }
            )
            return deferred.await()
        }

        override suspend fun onSolveUnsafeDeviceLoginVerify(bot: Bot, url: String): String? {
            updateServiceState(ParaboxKey.STATE_PAUSE, "请遵照提示完成身份验证")
            val deferred = CompletableDeferred<String>()
            sendRequest(
                request = ConnService.REQUEST_SOLVE_UNSAFE_DEVICE_LOGIN_VERIFY,
                client = ParaboxKey.CLIENT_CONTROLLER,
                extra = Bundle().apply {
                    putString("url", url)
                },
                timeoutMillis = 300000,
                onResult = {
                    if (it is ParaboxResult.Success) {
                        updateServiceState(ParaboxKey.STATE_LOADING, "正在尝试登陆")
                        it.obj.getString("result").also {
                            if (it != null) deferred.complete(it)
                            else throw NoSuchElementException("result not found")
                        }
                    } else {
                        val errMessage = when ((it as ParaboxResult.Fail).errorCode) {
                            ParaboxKey.ERROR_RESOURCE_NOT_FOUND -> "必要资源丢失"
                            ParaboxKey.ERROR_DISCONNECTED -> "与服务的连接断开"
                            ParaboxKey.ERROR_REPEATED_CALL -> "重复正在执行的操作"
                            ParaboxKey.ERROR_TIMEOUT -> "操作超时"
                            else -> "未知错误"
                        }
                        deferred.completeExceptionally(IOException("result transmission failed"))
                        updateServiceState(ParaboxKey.STATE_ERROR, errMessage)
                    }
                }
            )
            return deferred.await()
        }

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