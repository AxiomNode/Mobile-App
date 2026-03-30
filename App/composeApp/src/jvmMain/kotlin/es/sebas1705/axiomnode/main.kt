package es.sebas1705.axiomnode

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {

    Window(
        onCloseRequest = ::exitApplication,
        title = "AxiomNode",
    ) {
        App()
    }
}