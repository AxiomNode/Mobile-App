package es.sebas1705.axiomnode.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.sebas1705.axiomnode.data.network.ConnectivityMonitor
import es.sebas1705.axiomnode.domain.models.GameType
import es.sebas1705.axiomnode.domain.models.User
import es.sebas1705.axiomnode.presentation.navigation.Destination
import es.sebas1705.axiomnode.presentation.navigation.Navigator
import es.sebas1705.axiomnode.ui.components.AppScaffold
import es.sebas1705.axiomnode.ui.components.EmptyState
import es.sebas1705.axiomnode.ui.components.GameCard
import es.sebas1705.axiomnode.ui.components.InlineInfoCard
import es.sebas1705.axiomnode.ui.components.LoadingState
import es.sebas1705.axiomnode.ui.components.SectionHeader
import es.sebas1705.axiomnode.ui.components.StatTile
import es.sebas1705.axiomnode.ui.components.SyncBanner
import es.sebas1705.axiomnode.ui.layout.LocalWindowSize
import es.sebas1705.axiomnode.ui.layout.WindowSize
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    user: User?,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val connectivityMonitor: ConnectivityMonitor = koinInject()
    val isOnline by connectivityMonitor.isOnline.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val windowSize = LocalWindowSize.current
    val horizontalGutter = when (windowSize) {
        WindowSize.COMPACT -> 16.dp
        WindowSize.MEDIUM -> 20.dp
        WindowSize.EXPANDED -> 24.dp
    }
    val sectionSpacing = when (windowSize) {
        WindowSize.COMPACT -> 16.dp
        WindowSize.MEDIUM -> 18.dp
        WindowSize.EXPANDED -> 20.dp
    }

    AppScaffold(
        title = greetingFor(user),
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalGutter),
            verticalArrangement = Arrangement.spacedBy(sectionSpacing),
        ) {
            Spacer(Modifier.height(8.dp))

            // Hero card – CTA principal para crear partida
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "¿Listo para jugar?",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Genera una partida nueva o continúa con las propuestas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilledTonalButton(
                            modifier = Modifier.weight(1f),
                            onClick = { navigator.push(Destination.Lobby(GameType.QUIZ)) },
                            shape = RoundedCornerShape(12.dp),
                        ) { Text("Nuevo Quiz") }
                        FilledTonalButton(
                            modifier = Modifier.weight(1f),
                            onClick = { navigator.push(Destination.Lobby(GameType.WORDPASS)) },
                            shape = RoundedCornerShape(12.dp),
                        ) { Text("Nuevo Wordpass") }
                    }
                }
            }

            // Sync banner
            if (state.pendingSyncCount > 0) {
                SyncBanner(
                    pendingCount = state.pendingSyncCount,
                    isSyncing = false,
                    onSync = {
                        if (!isOnline) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Sin conexión: resultados se enviarán al reconectar")
                            }
                            return@SyncBanner
                        }
                        viewModel.syncPending { msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    },
                    message = if (!isOnline) "Modo offline: sincronización pausada" else null,
                )
            }

            if (!isOnline) {
                InlineInfoCard(
                    message = "Sin conexión. Mostrando contenido local y partidas jugadas.",
                )
            }

            // Stats summary
            state.stats?.let { stats ->
                if (stats.totalGames > 0) {
                    SectionHeader(title = "Tu progreso")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile(
                            value = "${stats.totalGames}",
                            label = "Partidas",
                            modifier = Modifier.weight(1f).height(96.dp),
                        )
                        StatTile(
                            value = "${stats.winRatePercentage}%",
                            label = "Victorias",
                            modifier = Modifier.weight(1f).height(96.dp),
                        )
                        StatTile(
                            value = "${stats.averageScore}",
                            label = "Puntos avg.",
                            modifier = Modifier.weight(1f).height(96.dp),
                        )
                    }
                }
            }

            // Featured games
            SectionHeader(title = "Partidas recomendadas")
            when {
                state.isLoading -> LoadingState(message = "Cargando partidas…")
                state.featuredGames.isEmpty() -> EmptyState(
                    title = "Sin contenido aún",
                    description = "Conéctate y genera tu primera partida.",
                    icon = Icons.Outlined.Casino,
                    actionLabel = "Generar Quiz",
                    onAction = { navigator.push(Destination.Lobby(GameType.QUIZ)) },
                )
                else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.featuredGames.forEach { game ->
                        GameCard(
                            game = game,
                            onClick = { navigator.push(Destination.Play(game.id)) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // Refresh when returning to Home
    LaunchedEffect(Unit) { viewModel.refresh() }

    // Try syncing pending results automatically when connectivity returns.
    LaunchedEffect(isOnline) {
        if (isOnline && state.pendingSyncCount > 0) {
            viewModel.syncPending { msg ->
                scope.launch { snackbarHostState.showSnackbar(msg) }
            }
        }
    }
}

private fun greetingFor(user: User?): String {
    val name = user?.displayName?.substringBefore(' ').orEmpty().ifBlank { "" }
    return if (name.isNotBlank()) "¡Hola, $name!" else "Inicio"
}

