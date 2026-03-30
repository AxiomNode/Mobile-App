package es.sebas1705.axiomnode.core

actual fun Any.logI(message: String) {
    val tag = this::class.simpleName ?: "AxiomNode"
    android.util.Log.i(tag, message)
}

actual fun Any.logE(message: String) {
    val tag = this::class.simpleName ?: "AxiomNode"
    android.util.Log.e(tag, message)
}

actual fun Any.logD(message: String) {
    val tag = this::class.simpleName ?: "AxiomNode"
    android.util.Log.d(tag, message)
}
