package com.ojhdtapp.miraipluginforparabox.toolkit

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class ParaboxMetadata(
    val command: Int,
    val timestamp: Long,
    val type: Int
) : Parcelable