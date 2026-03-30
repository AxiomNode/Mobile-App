package es.sebas1705.axiomnode.core

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSinceReferenceDate

private const val SECONDS_FROM_1970_TO_2001: Double = 978307200.0

actual fun currentTimeMillis(): Long {
    val seconds = NSDate().timeIntervalSinceReferenceDate + SECONDS_FROM_1970_TO_2001
    return (seconds * 1000.0).toLong()
}
