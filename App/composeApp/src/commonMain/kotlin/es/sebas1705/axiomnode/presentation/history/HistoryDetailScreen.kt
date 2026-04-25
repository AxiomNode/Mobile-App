package es.sebas1705.axiomnode.presentation.history

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
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.sebas1705.axiomnode.domain.models.Game
import es.sebas1705.axiomnode.domain.usecases.GamesUseCase
import es.sebas1705.axiomnode.presentation.navigation.Navigator
import es.sebas1705.axiomnode.ui.components.AppScaffold
import es.sebas1705.axiomnode.ui.components.DetailChip
import es.sebas1705.axiomnode.ui.components.EmptyState
import es.sebas1705.axiomnode.ui.components.GameTypeBadge
import es.sebas1705.axiomnode.ui.components.LoadingState
import es.sebas1705.axiomnode.ui.layout.LocalWindowSize
import es.sebas1705.axiomnode.ui.layout.WindowSize
import org.koin.compose.koinInject

/**
 * Read-only detail view for a played game (uses cached data only).
 */
@Composable
fun HistoryDetailScreen(
    gameId: String,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val gamesUseCase: GamesUseCase = koinInject()
    var game by remember { mutableStateOf<Game?>(null) }
    var loading by remember { mutableStateOf(true) }
    var notFound by remember { mutableStateOf(false) }
    val windowSize = LocalWindowSize.current
    val horizontalGutter = when (windowSize) {
        WindowSize.COMPACT -> 16.dp
        WindowSize.MEDIUM -> 20.dp
        WindowSize.EXPANDED -> 24.dp
    }

    LaunchedEffect(gameId) {
        loading = true
        gamesUseCase.getCachedGameById(gameId)
            .onSuccess { cached ->
                if (cached == null) notFound = true else game = cached
            }
            .onFailure { notFound = true }
        loading = false
    }

    AppScaffold(
        title = game?.categoryName ?: "Detalle",
        modifier = modifier,
        onBack = { navigator.pop() },
    ) { _ ->
        when {
            loading -> LoadingState(message = "Cargando partida…")
            notFound || game == null -> EmptyState(
                title = "Sin caché local",
                description = "Esta partida no se conserva en el dispositivo.",
                icon = Icons.Outlined.Inbox,
            )
            else -> {
                val g = game!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = horizontalGutter),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    GameTypeBadge(g.gameType)
                                    DetailChip(g.language.uppercase())
                                    DetailChip("${g.questions.size} preguntas")
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = g.categoryName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                    items(g.questions, key = { it.id }) { q ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = q.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Respuesta: ${q.correctAnswer}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

