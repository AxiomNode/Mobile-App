package es.sebas1705.axiomnode.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp

/**
 * Lightweight window size class derived from the available width.
 * Avoids depending on `material3-window-size-class` on KMP for now.
 */
enum class WindowSize { COMPACT, MEDIUM, EXPANDED }

fun windowSizeFor(maxWidth: Dp): WindowSize = when {
    maxWidth.value < 600f -> WindowSize.COMPACT
    maxWidth.value < 840f -> WindowSize.MEDIUM
    else -> WindowSize.EXPANDED
}

val LocalWindowSize = staticCompositionLocalOf { WindowSize.COMPACT }

