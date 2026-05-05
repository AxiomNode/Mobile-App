package es.sebas1705.axiomnode.presentation.navigation

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    // iOS navigation/back gesture is handled by platform containers.
}
