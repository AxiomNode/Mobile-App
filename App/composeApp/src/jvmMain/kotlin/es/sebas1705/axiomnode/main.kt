package es.sebas1705.axiomnode

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.compose.resources.painterResource
import es.sebas1705.axiomnode.resources.Res
import es.sebas1705.axiomnode.resources.app_icon

fun main() = application {

    Window(
        onCloseRequest = ::exitApplication,
        title = "AxiomNode",
        icon = painterResource(Res.drawable.app_icon),
    ) {
        App()
    }
}

