package es.sebas1705.axiomnode.presentation.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.sebas1705.axiomnode.domain.models.GameStats
import es.sebas1705.axiomnode.domain.usecases.GamesUseCase
import es.sebas1705.axiomnode.presentation.auth.AuthViewModel
import es.sebas1705.axiomnode.presentation.navigation.Destination
import es.sebas1705.axiomnode.presentation.navigation.Navigator
import es.sebas1705.axiomnode.ui.components.AppScaffold
import es.sebas1705.axiomnode.ui.components.ConfirmDialog
import es.sebas1705.axiomnode.ui.components.SectionHeader
import es.sebas1705.axiomnode.ui.components.StatTile
import es.sebas1705.axiomnode.ui.components.WinRateBar
import es.sebas1705.axiomnode.ui.layout.LocalWindowSize
import es.sebas1705.axiomnode.ui.layout.WindowSize
import org.koin.compose.koinInject

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    navigator: Navigator,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by authViewModel.state.collectAsStateWithLifecycle()
    val gamesUseCase: GamesUseCase = koinInject()

    var stats by remember { mutableStateOf<GameStats?>(null) }
    var confirmLogout by remember { mutableStateOf(false) }
    val windowSize = LocalWindowSize.current
    val horizontalGutter = when (windowSize) {
        WindowSize.COMPACT -> 16.dp
        WindowSize.MEDIUM -> 20.dp
        WindowSize.EXPANDED -> 24.dp
    }

    LaunchedEffect(Unit) {
        authViewModel.syncProfileOnEnter()
        gamesUseCase.getGameStats().onSuccess { stats = it }
    }

    if (confirmLogout) {
        ConfirmDialog(
            title = "Cerrar sesión",
            message = "¿Seguro que quieres cerrar sesión? Tus datos locales se mantendrán.",
            confirmLabel = "Cerrar sesión",
            onConfirm = {
                confirmLogout = false
                authViewModel.signOut()
                onSignOut()
            },
            onDismiss = { confirmLogout = false },
        )
    }

    AppScaffold(title = "Perfil", modifier = modifier) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalGutter)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // ── User card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = state.user?.displayName?.take(2)?.uppercase() ?: "?",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = state.user?.displayName ?: "Usuario",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = state.user?.email ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = state.user?.role?.name ?: "GAMER",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            // ── Resumen stats
            stats?.takeIf { it.totalGames > 0 }?.let { gs ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        WinRateBar(winRate = gs.winRate)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatTile("${gs.totalGames}", "Partidas", Modifier.weight(1f).height(80.dp))
                            StatTile("${gs.wins}", "Victorias", Modifier.weight(1f).height(80.dp))
                            StatTile("${gs.averageScore}", "Avg.", Modifier.weight(1f).height(80.dp))
                        }
                    }
                }
            }

            // ── Acciones
            SectionHeader(title = "Cuenta")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column {
                    NavRow(
                        title = "Ajustes",
                        icon = Icons.Outlined.Settings,
                        onClick = { navigator.push(Destination.Settings) },
                    )
                    NavRow(
                        title = "Acerca de",
                        icon = Icons.Outlined.Info,
                        onClick = { navigator.push(Destination.About) },
                    )
                }
            }

            OutlinedButton(
                onClick = { confirmLogout = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = "Cerrar Sesión",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun NavRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}

/**
 * Returns a human-readable platform name. Implemented per platform.
 */
expect fun getPlatformName(): String

