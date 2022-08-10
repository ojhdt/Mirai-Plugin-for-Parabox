package com.ojhdtapp.miraipluginforparabox.data.remote.dto

import android.os.Parcelable
import com.ojhdtapp.miraipluginforparabox.domain.model.Profile
import com.ojhdtapp.miraipluginforparabox.domain.model.PluginConnection
import com.ojhdtapp.miraipluginforparabox.domain.model.message_content.MessageContent
import kotlinx.parcelize.Parcelize

@Parcelize
data class MessageDto(
    val contents: List<MessageContent>,
    val profile: Profile,
    val subjectProfile: Profile,
    val timestamp: Long,
    val pluginConnection: PluginConnection
) : Parcelable {

    //Unused
//    suspend fun toContactEntityWithUnreadMessagesNumUpdate(dao: ContactDao): ContactEntity {
//        return ContactEntity(
//            profile = subjectProfile,
//            latestMessage = LatestMessage(
//                contents.getContentString(),
//                timestamp,
//                (withContext(Dispatchers.IO) {
//                    dao.getContactById(pluginConnection.objectId)?.latestMessage?.unreadMessagesNum
//                }
//                    ?: 0) + 1
//            ),
//            contactId = pluginConnection.objectId,
//            senderId = pluginConnection.objectId,
//            tags = emptyList()
//        )
//    }
//
//    fun toContactEntity(): ContactEntity {
//        return ContactEntity(
//            profile = subjectProfile,
//            latestMessage = LatestMessage(
//                content = contents.getContentString(),
//                timestamp = timestamp,
//                unreadMessagesNum = 0
//            ),
//            contactId = pluginConnection.objectId,
//            senderId = pluginConnection.objectId,
//            isHidden = false,
//            isPinned = false,
//            isArchived = false,
//            enableNotifications = true,
//            tags = emptyList()
//        )
//    }
//
//    fun toMessageEntity(): MessageEntity {
//        return MessageEntity(
//            contents = contents,
//            profile = profile,
//            timestamp = timestamp,
//        )
//    }

//    fun getContactMessageCrossRef(): ContactMessageCrossRef {
//        return ContactMessageCrossRef(
//            pluginConnection.objectId,
//            id
//        )
//    }
}