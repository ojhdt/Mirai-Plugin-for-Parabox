package com.ojhdtapp.miraipluginforparabox.domain.util

import com.ojhdtapp.messagedto.message_content.MessageContent
import com.ojhdtapp.miraipluginforparabox.core.util.FaceMap
import com.ojhdtapp.miraipluginforparabox.domain.service.ConnService
import net.mamoe.mirai.contact.FileSupported
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl

suspend fun MessageChain.toMessageContentList(group: Group? = null): List<MessageContent> {
    return this.filter {
        it is QuoteReply || (it is net.mamoe.mirai.message.data.MessageContent
                && it.contentToString().isNotEmpty())
    }.map {
        when (it) {
            is QuoteReply -> {
                val quoteMessage = it.source.originalMessage
                com.ojhdtapp.messagedto.message_content.QuoteReply(
                    getMessageId(quoteMessage.ids),
                    quoteMessage.toMessageContentList()
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
            is Audio -> com.ojhdtapp.messagedto.message_content.Audio(
                (it as OnlineAudio).urlForDownload,
                (it as OnlineAudio).length,
                (it as OnlineAudio).filename,
                (it as OnlineAudio).fileSize,
            )
            is FileMessage -> {
                group?.let { group ->
                    it.toAbsoluteFile(group)?.let { file ->
                        com.ojhdtapp.messagedto.message_content.File(
                            url = file.getUrl(),
                            name = file.name,
                            size = file.size,
                            lastModifiedTime = file.lastModifiedTime,
                            expiryTime = file.lastModifiedTime
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

