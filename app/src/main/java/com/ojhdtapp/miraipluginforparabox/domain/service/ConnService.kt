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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import moe.ore.silk.AudioUtils
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

class ConnService : ParaboxService() {
    @Inject
    lateinit var repository: MainRepository
    companion object {
        init {
            System.loadLibrary("silkcodec")
        }
    }
}