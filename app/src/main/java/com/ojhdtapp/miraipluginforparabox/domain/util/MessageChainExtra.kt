package com.ojhdtapp.miraipluginforparabox.domain.util

import android.content.Context
import android.content.Intent
import android.util.Log
import com.ojhdtapp.messagedto.message_content.MessageContent
import com.ojhdtapp.miraipluginforparabox.core.util.FaceMap
import com.ojhdtapp.miraipluginforparabox.core.util.FileUtil
import com.ojhdtapp.miraipluginforparabox.data.local.MiraiMessageDao
import com.ojhdtapp.miraipluginforparabox.data.remote.api.FileDownloadService
import com.ojhdtapp.miraipluginforparabox.domain.repository.MainRepository
import com.ojhdtapp.miraipluginforparabox.domain.service.ConnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.ore.silk.AudioUtils
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.FileSupported
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.file.AbsoluteFileFolder.Companion.extension
import net.mamoe.mirai.contact.file.AbsoluteFileFolder.Companion.nameWithoutExtension
import net.mamoe.mirai.contact.getMember
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.message.code.MiraiCode
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import java.io.File
import java.io.IOException

suspend fun MessageChain.toMessageContentList(
    context: Context,
    downloadService: FileDownloadService,
    group: Group? = null,
    bot: Bot,
    exceptQuote: Boolean = false,
    repository: MainRepository
): List<MessageContent> {
    return this
        .filter {
            if (exceptQuote) (it !is QuoteReply) else true
        }
        .filter {
            it is QuoteReply || (it is net.mamoe.mirai.message.data.MessageContent)
        }
        .map {
            when (it) {
                is QuoteReply -> {
                    val quoteMessageId = getMessageId(it.source.ids)
//                    val quoteMessage = it.source.originalMessage
//                quoteMessage.bot.asStranger.nameCardOrNick
                    val quoteMessage = quoteMessageId?.let {
                        withContext(Dispatchers.IO) {
                            repository.getMiraiMessageById(it).let {
                                if (it != null) {
                                    MiraiCode.deserializeMiraiCode(it.miraiCode)
                                } else null
                            }
                        }
                    } ?: it.source.originalMessage
                    val nick = try {
                        group?.getMember(it.source.fromId)?.nameCardOrNick
                            ?: bot.getFriend(it.source.fromId)?.nameCardOrNick
                            ?: bot.getStranger(it.source.fromId)?.nameCardOrNick
                            ?: it.source.fromId.toString()
                    } catch (e: NoSuchElementException) {
                        e.printStackTrace()
                        null
                    }
                    com.ojhdtapp.messagedto.message_content.QuoteReply(
                        nick,
                        "${it.source.time}000".toLong(),
                        quoteMessageId,
                        quoteMessage.toMessageContentList(
                            context = context,
                            downloadService = downloadService,
                            bot = bot,
                            exceptQuote = true,
                            repository = repository
                        )
                    )
                }
                is PlainText -> com.ojhdtapp.messagedto.message_content.PlainText(
                    it.content
                )
                is Image -> com.ojhdtapp.messagedto.message_content.Image(
                    it.queryUrl(),
                    it.width,
                    it.height
                )
                is FlashImage -> com.ojhdtapp.messagedto.message_content.Image(
                    it.image.queryUrl(),
                    it.image.width,
                    it.image.height
                )
                is At -> com.ojhdtapp.messagedto.message_content.At(
                    it.target, it.getDisplay(group)
                )
                is AtAll -> com.ojhdtapp.messagedto.message_content.AtAll
                is Face -> com.ojhdtapp.messagedto.message_content.PlainText(
                    FaceMap.query(it.id) ?: it.content
                )
                is Audio -> {
                    val url = (it as OnlineAudio).urlForDownload
                    Log.d("parabox", url)
                    val tempPath =
                        File(context.externalCacheDir, it.filename.replace(".amr", ".silk"))
                    val uri = try {
                        coroutineScope {
                            withContext(Dispatchers.IO) {
                                downloadService.download(url).let {
                                    if (it.isSuccessful) {
                                        it.body().let {
                                            FileUtil.saveFile(body = it, path = tempPath)
                                            AudioUtils.init(context.externalCacheDir!!.absolutePath)
                                            AudioUtils.silkToMp3(tempPath).let {
                                                FileUtil.getUriOfFile(context, it).apply {
                                                    context.grantUriPermission(
                                                        "com.ojhdtapp.parabox",
                                                        this,
                                                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                    )
                                                }
                                            }
//                                            AudioUtils.silkToMp3(tempPath).let {
//                                                FileUtil.getUriOfFile(context, it).apply {
//                                                    context.grantUriPermission(
//                                                        "com.ojhdtapp.parabox",
//                                                        this,
//                                                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
//                                                    )
//                                                }
//                                            }
                                        }
                                    } else null
                                }
                            }
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        null
                    }
                    com.ojhdtapp.messagedto.message_content.Audio(
                        url,
                        it.length,
                        it.filename,
                        it.fileSize,
                        uri,
                    )
                }
                is FileMessage -> {
                    group?.let { group ->
                        it.toAbsoluteFile(group)?.let { file ->
                            com.ojhdtapp.messagedto.message_content.File(
                                url = file.getUrl(),
                                name = file.name,
                                extension = file.extension,
                                size = file.size,
                                lastModifiedTime = file.lastModifiedTime,
                                expiryTime = file.lastModifiedTime,
                            )
                        }
                    }
                        ?: com.ojhdtapp.messagedto.message_content.PlainText(
                            it.content
                        )
                }
                else -> com.ojhdtapp.messagedto.message_content.PlainText(
                    it.content
                )
            }
        }
}

fun getMessageId(ids: IntArray): Long? {
    return ids.firstOrNull()?.let { "${ConnService.connectionType}$it".toLong() }
}

