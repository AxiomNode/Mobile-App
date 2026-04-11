package es.sebas1705.axiomnode.presentation.profile

import android.os.Build

actual fun getPlatformName(): String = "Android ${Build.VERSION.SDK_INT}"

