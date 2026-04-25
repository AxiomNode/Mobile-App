package es.sebas1705.axiomnode.presentation.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.sebas1705.axiomnode.domain.models.GameType
import es.sebas1705.axiomnode.presentation.navigation.Navigator
import es.sebas1705.axiomnode.ui.components.AppScaffold
import es.sebas1705.axiomnode.ui.components.EmptyState
import es.sebas1705.axiomnode.ui.components.LoadingState
import es.sebas1705.axiomnode.ui.components.SectionHeader
import es.sebas1705.axiomnode.ui.components.StatTile
import es.sebas1705.axiomnode.ui.components.WinRateBar
import es.sebas1705.axiomnode.ui.layout.LocalWindowSize
import es.sebas1705.axiomnode.ui.layout.WindowSize

@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    @Suppress("UNUSED_PARAMETER") navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val windowSize = LocalWindowSize.current
    val horizontalGutter = when (windowSize) {
        WindowSize.COMPACT -> 16.dp
        WindowSize.MEDIUM -> 20.dp
        WindowSize.EXPANDED -> 24.dp
    }

    AppScaffold(title = "Estadísticas", modifier = modifier) { _ ->
        when {
            state.isLoading -> LoadingState(message = "Calculando…")
            state.global == null || state.global!!.totalGames == 0 -> EmptyState(
                title = "Sin partidas todavía",
                description = "Juega tu primera partida para ver estadísticas detalladas.",
                icon = Icons.Outlined.BarChart,
            )
            else -> {
                val stats = state.global!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = horizontalGutter),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Spacer(Modifier.height(8.dp))

                    // Win rate hero
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            WinRateBar(winRate = stats.winRate)
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                StatTile("${stats.wins}", "Victorias", Modifier.weight(1f).height(96.dp))
                                StatTile("${stats.draws}", "Empates", Modifier.weight(1f).height(96.dp))
                                StatTile("${stats.losses}", "Derrotas", Modifier.weight(1f).height(96.dp))
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                StatTile("${stats.totalGames}", "Partidas", Modifier.weight(1f).height(96.dp))
                                StatTile("${stats.averageScore}", "Puntos avg.", Modifier.weight(1f).height(96.dp))
                                StatTile(formatPlayTime(stats.totalPlayTimeSeconds), "Tiempo", Modifier.weight(1f).height(96.dp))
                            }
                        }
                    }

                    // Por tipo de juego
                    if (state.byType.isNotEmpty()) {
                        SectionHeader(title = "Por tipo de juego")
                        state.byType.forEach { (type, ts) ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                ),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = if (type == GameType.QUIZ) "Quiz" else "Wordpass",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "${ts.totalGames} partidas · ${ts.wins} victorias · avg ${ts.averageScore} pts",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    // Por categoría
                    if (state.byCategory.isNotEmpty()) {
                        SectionHeader(title = "Por categoría")
                        state.byCategory.forEach { cat ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                ),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = cat.categoryName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "${cat.totalGames} partidas · ${cat.wins} victorias · avg ${cat.averageScore} pts",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

private fun formatPlayTime(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

