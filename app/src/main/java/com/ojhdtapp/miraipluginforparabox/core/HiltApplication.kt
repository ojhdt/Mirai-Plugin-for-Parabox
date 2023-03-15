package com.ojhdtapp.miraipluginforparabox.core

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HiltApplication : Application() {
}

const val MIRAI_CORE_VERSION = "2.15.0-dev-24"