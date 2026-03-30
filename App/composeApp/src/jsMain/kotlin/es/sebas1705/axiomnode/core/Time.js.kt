package es.sebas1705.axiomnode.core

actual fun currentTimeMillis(): Long = kotlin.js.Date.now().toLong()

