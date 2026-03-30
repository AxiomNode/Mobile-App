package es.sebas1705.axiomnode.core

import kotlin.js.ExperimentalWasmJsInterop

// Kotlin/Wasm: evita js("...") directo, usa external.
private external fun dateNow(): Double

@OptIn(ExperimentalWasmJsInterop::class)
@Suppress("unused")
private val __initDateNow: Int = js("globalThis.dateNow = () => Date.now(); 0")

actual fun currentTimeMillis(): Long = dateNow().toLong()
