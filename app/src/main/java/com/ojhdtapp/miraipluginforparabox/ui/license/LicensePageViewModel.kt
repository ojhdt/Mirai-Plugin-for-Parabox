package com.ojhdtapp.miraipluginforparabox.ui.license

import androidx.lifecycle.ViewModel

class LicensePageViewModel : ViewModel() {
    // Licenses
    val licenseList = listOf<License>(
        License(
            "Accompanist",
            "https://github.com/google/accompanist/blob/main/LICENSE",
            "Apache License 2.0",
        ),
        License(
            "AndroidX",
            "https://developer.android.com/jetpack/androidx",
            "Apache License 2.0"
        ),
        License(
            "AndroidX DataStore",
            "https://developer.android.com/jetpack/androidx/releases/datastore",
            "Apache License 2.0"
        ),
        License(
            "AndroidX Lifecycle",
            "https://developer.android.com/jetpack/androidx/releases/lifecycle",
            "Apache License 2.0"
        ),
        License(
            "AndroidX Compose",
            "https://developer.android.com/jetpack/androidx/releases/compose",
            "Apache License 2.0"
        ),
        License(
            "AndroidX Compose Material",
            "https://developer.android.com/jetpack/androidx/releases/compose-material",
            "Apache License 2.0"
        ),
        License(
            "Coil",
            "https://github.com/coil-kt/coil/blob/main/LICENSE.txt",
            "Apache License 2.0"
        ),
        License(
            "Kotlin",
            "https://github.com/JetBrains/kotlin",
            "Apache License 2.0"
        ),
        License(
            "Retrofit",
            "https://github.com/square/retrofit/blob/master/LICENSE.txt",
            "Apache License 2.0"
        ),
        License(
            "Compose Destinations",
            "https://github.com/raamcosta/compose-destinations/blob/main/LICENSE.txt",
            "Apache License 2.0"
        ),
        License(
            "gson",
            "https://github.com/google/gson/blob/master/LICENSE",
            "Apache License 2.0"
        ),
        License(
            "mirai",
            "https://github.com/mamoe/mirai/blob/dev/LICENSE",
            "GNU Affero General Public License v3.0"
        ),
        License(
            "ffmpeg-kit",
            "https://github.com/arthenica/ffmpeg-kit/blob/main/LICENSE",
            "GNU Lesser General Public License v3.0"
        )
    )
}