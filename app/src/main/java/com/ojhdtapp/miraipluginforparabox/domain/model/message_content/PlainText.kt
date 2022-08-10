package com.ojhdtapp.miraipluginforparabox.domain.model.message_content

import com.ojhdtapp.miraipluginforparabox.domain.model.message_content.MessageContent
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlainText(val text: String) : MessageContent {
    val type = MessageContent.PLAIN_TEXT
    override fun getContentString(): String {
        return text
    }
}
