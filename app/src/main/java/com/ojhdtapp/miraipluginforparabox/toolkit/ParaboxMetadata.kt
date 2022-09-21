package com.ojhdtapp.miraipluginforparabox.toolkit

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class ParaboxMetadata(
    val commandOrRequest: Int,
    val timestamp: Long,
    val sender: Int
) : Parcelable