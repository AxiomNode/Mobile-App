package es.sebas1705.axiomnode.presentation.profile

import platform.UIKit.UIDevice

actual fun getPlatformName(): String =
    "${UIDevice.currentDevice.systemName} ${UIDevice.currentDevice.systemVersion}"

