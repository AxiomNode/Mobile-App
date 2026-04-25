package es.sebas1705.axiomnode.presentation.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.sebas1705.axiomnode.data.network.ConnectivityMonitor
import es.sebas1705.axiomnode.domain.models.GameResult
import es.sebas1705.axiomnode.presentation.navigation.Destination
import es.sebas1705.axiomnode.presentation.navigation.Navigator
import es.sebas1705.axiomnode.ui.components.AppScaffold
import es.sebas1705.axiomnode.ui.components.DetailChip
import es.sebas1705.axiomnode.ui.components.EmptyState
import es.sebas1705.axiomnode.ui.components.GameTypeBadge
import es.sebas1705.axiomnode.ui.components.InlineInfoCard
import es.sebas1705.axiomnode.ui.components.LoadingState
import es.sebas1705.axiomnode.ui.components.OutcomeBadge
import es.sebas1705.axiomnode.ui.components.SyncBanner
import es.sebas1705.axiomnode.ui.layout.LocalWindowSize
import es.sebas1705.axiomnode.ui.layout.WindowSize
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val connectivityMonitor: ConnectivityMonitor = koinInject()
    val isOnline by connectivityMonitor.isOnline.collectAsStateWithLifecycle()
    val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val windowSize = LocalWindowSize.current
    val horizontalGutter = when (windowSize) {
        WindowSize.COMPACT -> 16.dp
        WindowSize.MEDIUM -> 20.dp
        WindowSize.EXPANDED -> 24.dp
    }

    LaunchedEffect(Unit) { viewModel.refresh() }

    AppScaffold(
        title = "Historial",
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    ) { _ ->
        Column(modifier = Modifier.fillMaxSize()) {
            if (!isOnline) {
                Spacer(Modifier.height(8.dp))
                InlineInfoCard(
                    message = "Sin conexión. El historial local está disponible y la sincronización se reanudará al reconectar.",
                    modifier = Modifier.padding(horizontal = horizontalGutter),
                )
            }

            if (state.pendingSyncCount > 0) {
                Spacer(Modifier.height(8.dp))
                SyncBanner(
                    pendingCount = state.pendingSyncCount,
                    isSyncing = state.isSyncing,
                    onSync = {
                        if (!isOnline) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Sin conexión: no se puede sincronizar ahora")
                            }
                            return@SyncBanner
                        }
                        viewModel.syncPending()
                    },
                    message = state.syncMessage ?: if (!isOnline) "Modo offline: sincronización pausada" else null,
                    modifier = Modifier.padding(horizontal = horizontalGutter),
                )
            }

            when {
                state.isLoading -> LoadingState(message = "Cargando historial…")
                state.results.isEmpty() -> EmptyState(
                    title = "Sin partidas todavía",
                    description = "Juega tu primera partida para ver el historial.",
                    icon = Icons.Outlined.History,
                )
                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = horizontalGutter),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item { Spacer(Modifier.height(8.dp)) }
                    items(state.results, key = { it.gameId + "-" + it.timestamp }) { result ->
                        HistoryCard(
                            result = result,
                            onClick = {
                                navigator.push(
                                    Destination.HistoryDetail(result.gameId, result.timestamp)
                                )
                            },
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }

    LaunchedEffect(isOnline) {
        if (isOnline && state.pendingSyncCount > 0) {
            viewModel.syncPending()
        }
    }
}

@Composable
private fun HistoryCard(result: GameResult, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.categoryName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GameTypeBadge(result.gameType)
                    DetailChip("${result.score} pts")
                    DetailChip(formatDuration(result.durationSeconds))
                }
            }
            OutcomeBadge(result.outcome)
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "${m}:${s.toString().padStart(2, '0')}"
}

