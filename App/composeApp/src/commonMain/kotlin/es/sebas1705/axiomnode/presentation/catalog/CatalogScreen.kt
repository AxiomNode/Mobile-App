package es.sebas1705.axiomnode.presentation.catalog

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.sebas1705.axiomnode.domain.models.GameType
import es.sebas1705.axiomnode.presentation.games.GamesViewModel
import es.sebas1705.axiomnode.presentation.navigation.Destination
import es.sebas1705.axiomnode.presentation.navigation.Navigator
import es.sebas1705.axiomnode.ui.components.AppScaffold
import es.sebas1705.axiomnode.ui.components.EmptyState
import es.sebas1705.axiomnode.ui.components.GameCard
import es.sebas1705.axiomnode.ui.components.InlineErrorCard
import es.sebas1705.axiomnode.ui.components.InlineInfoCard
import es.sebas1705.axiomnode.ui.components.LoadingState
import es.sebas1705.axiomnode.ui.layout.LocalWindowSize
import es.sebas1705.axiomnode.ui.layout.WindowSize

@Composable
fun CatalogScreen(
    viewModel: GamesViewModel,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val windowSize = LocalWindowSize.current
    val horizontalGutter = when (windowSize) {
        WindowSize.COMPACT -> 16.dp
        WindowSize.MEDIUM -> 20.dp
        WindowSize.EXPANDED -> 24.dp
    }
    val itemSpacing = when (windowSize) {
        WindowSize.COMPACT -> 12.dp
        WindowSize.MEDIUM -> 14.dp
        WindowSize.EXPANDED -> 16.dp
    }

    LaunchedEffect(Unit) { viewModel.loadRandomGames() }

    AppScaffold(
        title = "Catálogo",
        modifier = modifier,
    ) { _ ->
        Column(modifier = Modifier.fillMaxSize()) {

            state.catalog?.let { catalog ->
                if (catalog.languages.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = horizontalGutter, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        catalog.languages.forEach { language ->
                            FilterChip(
                                selected = state.selectedLanguage == language.code,
                                onClick = {
                                    viewModel.setSelectedLanguage(language.code)
                                    viewModel.loadRandomGames()
                                },
                                label = { Text(language.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                ),
                            )
                        }
                    }
                }
            }

            // Filtros (categorías)
            state.catalog?.let { catalog ->
                if (catalog.categories.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = horizontalGutter, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = state.selectedCategoryId == null,
                            onClick = {
                                viewModel.setSelectedCategory(null)
                                viewModel.loadRandomGames()
                            },
                            label = { Text("Todas") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                        catalog.categories.forEach { category ->
                            FilterChip(
                                selected = state.selectedCategoryId == category.id,
                                onClick = {
                                    viewModel.setSelectedCategory(category.id)
                                    viewModel.loadRandomGames()
                                },
                                label = { Text(category.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                            )
                        }
                    }
                }
            }

            state.error?.let {
                InlineErrorCard(
                    message = it,
                    modifier = Modifier.padding(horizontal = horizontalGutter, vertical = 4.dp),
                )
            }
            state.contentAdvice?.let {
                InlineInfoCard(
                    message = it,
                    modifier = Modifier.padding(horizontal = horizontalGutter, vertical = 4.dp),
                )
            }

            when {
                state.isLoading && state.games.isEmpty() ->
                    LoadingState(message = "Cargando partidas…")
                state.games.isEmpty() -> EmptyState(
                    title = "Sin partidas",
                    description = "Pulsa “+” para generar una nueva.",
                    icon = Icons.Outlined.SportsEsports,
                )
                else -> {
                    val columns = when (windowSize) {
                        WindowSize.COMPACT -> 1
                        WindowSize.MEDIUM -> 2
                        WindowSize.EXPANDED -> 3
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = horizontalGutter),
                        verticalArrangement = Arrangement.spacedBy(itemSpacing),
                        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp),
                    ) {
                        items(state.games, key = { it.id }) { game ->
                            GameCard(
                                game = game,
                                onClick = { navigator.push(Destination.Play(game.id)) },
                            )
                        }
                    }
                }
            }
        }

        // FAB to load more
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = horizontalGutter, vertical = 20.dp),
            contentAlignment = androidx.compose.ui.Alignment.BottomEnd,
        ) {
            ExtendedFloatingActionButton(
                onClick = { viewModel.loadRandomGames() },
                shape = RoundedCornerShape(16.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                    )
                },
                text = { Text("Más partidas") },
            )
        }
    }
}

