// File: `composeApp/src/jsMain/kotlin/es/sebas1705/axiomnode/core/Logs.kt`
package es.sebas1705.axiomnode.core

actual fun Any.logI(message: String) {
    val tag = this::class.simpleName ?: "AxiomNode"
    println("[I] $tag: $message") // Kotlin/JS println -> console
}

actual fun Any.logE(message: String) {
    val tag = this::class.simpleName ?: "AxiomNode"
    println("[E] $tag: $message")
}

actual fun Any.logD(message: String) {
    val tag = this::class.simpleName ?: "AxiomNode"
    println("[D] $tag: $message")
}
