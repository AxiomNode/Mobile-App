package es.sebas1705.axiomnode.presentation.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.sebas1705.axiomnode.domain.models.GameOutcome
import es.sebas1705.axiomnode.domain.models.GameResult
import es.sebas1705.axiomnode.domain.usecases.GamesUseCase
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
) {
    val gamesUseCase: GamesUseCase = koinInject()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()

    var results by remember { mutableStateOf<List<GameResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var pendingSyncCount by remember { mutableStateOf(0) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        gamesUseCase.getRecentResults(50)
            .onSuccess { results = it }
        pendingSyncCount = gamesUseCase.getPendingSyncCount()
        isLoading = false
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Historial",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Loading
            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                }
            }

            // Sync banner
            AnimatedVisibility(
                visible = !isLoading && pendingSyncCount > 0,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🔄 $pendingSyncCount resultado${if (pendingSyncCount != 1) "s" else ""} sin sincronizar",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                            syncMessage?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    isSyncing = true
                                    syncMessage = null
                                    gamesUseCase.syncPendingResults()
                                        .onSuccess { count ->
                                            syncMessage = "$count sincronizado${if (count != 1) "s" else ""}"
                                            pendingSyncCount = gamesUseCase.getPendingSyncCount()
                                        }
                                        .onFailure { e ->
                                            syncMessage = "Error: ${e.message}"
                                        }
                                    isSyncing = false
                                }
                            },
                            enabled = !isSyncing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary,
                            ),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onTertiary,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text("Sincronizar", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // Empty state
            if (!isLoading && results.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "📊",
                            style = MaterialTheme.typography.displayLarge,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Sin partidas todavía",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Juega tu primera partida para ver el historial",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }

            // Results list
            if (!isLoading && results.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(results) { result ->
                        HistoryCard(result = result)
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(result: GameResult) {
    val outcomeEmoji = when (result.outcome) {
        GameOutcome.WON -> "🏆"
        GameOutcome.DRAW -> "🤔"
        GameOutcome.LOST -> "💪"
    }
    val outcomeLabel = when (result.outcome) {
        GameOutcome.WON -> "Victoria"
        GameOutcome.DRAW -> "Empate"
        GameOutcome.LOST -> "Derrota"
    }
    val outcomeColor = when (result.outcome) {
        GameOutcome.WON -> MaterialTheme.colorScheme.primaryContainer
        GameOutcome.DRAW -> MaterialTheme.colorScheme.tertiaryContainer
        GameOutcome.LOST -> MaterialTheme.colorScheme.errorContainer
    }
    val onOutcomeColor = when (result.outcome) {
        GameOutcome.WON -> MaterialTheme.colorScheme.onPrimaryContainer
        GameOutcome.DRAW -> MaterialTheme.colorScheme.onTertiaryContainer
        GameOutcome.LOST -> MaterialTheme.colorScheme.onErrorContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            text = result.gameType.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            text = "${result.score} pts",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            text = formatDuration(result.durationSeconds),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            Surface(
                color = outcomeColor,
                shape = RoundedCornerShape(10.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(text = outcomeEmoji, style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = outcomeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = onOutcomeColor,
                    )
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val min = seconds / 60
    val sec = seconds % 60
    return "${min}:${sec.toString().padStart(2, '0')}"
}

