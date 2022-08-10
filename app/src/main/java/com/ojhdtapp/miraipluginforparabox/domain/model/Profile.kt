package com.ojhdtapp.miraipluginforparabox.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Profile(
    val name: String,
    val avatar: String?
) : Parcelable