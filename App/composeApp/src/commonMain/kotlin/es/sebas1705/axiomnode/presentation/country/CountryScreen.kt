package es.sebas1705.axiomnode.presentation.country

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.sebas1705.axiomnode.core.logE
import es.sebas1705.axiomnode.domain.models.Country
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryScreen(
    modifier: Modifier = Modifier,
    countryViewModel: CountryViewModel = koinViewModel(),
) {
    val countryState by countryViewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Countries ${countryState.nCountries}") },
                actions = {
                    TextButton(
                        onClick = { countryViewModel.refresh() },
                        enabled = !countryState.isLoading && !countryState.isRefreshing
                    ) {
                        Text("Refrescar")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        val triggerPx = 140f
        var pullAccum by remember { mutableStateOf(0f) }

        val listState = rememberLazyListState()
        val gridState = rememberLazyGridState()
        val scope = rememberCoroutineScope()

        val nestedConnection = remember(countryState.isRefreshing, countryState.isLoading) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (countryState.isRefreshing || countryState.isLoading) return Offset.Zero

                    val pullingDown = available.y > 0
                    if (!pullingDown) return Offset.Zero

                    // Solo acumulamos si estamos en el top del scroll
                    val isAtTopList = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                    val isAtTopGrid = gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
                    if (!isAtTopList && !isAtTopGrid) return Offset.Zero

                    pullAccum += available.y
                    return Offset(0f, 0f)
                }

                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    if (countryState.isRefreshing || countryState.isLoading) return Offset.Zero

                    val pullingDown = available.y > 0
                    if (!pullingDown) return Offset.Zero

                    val isAtTopList = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                    val isAtTopGrid = gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
                    if (!isAtTopList && !isAtTopGrid) return Offset.Zero

                    pullAccum += available.y
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (!countryState.isRefreshing && !countryState.isLoading && pullAccum > triggerPx) {
                        scope.launch { countryViewModel.refresh() }
                    }
                    pullAccum = 0f
                    return super.onPreFling(available)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(nestedConnection)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (countryState.isRefreshing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val isExpanded = maxWidth >= 840.dp

                    when {
                        countryState.isLoading -> LoadingState(Modifier.fillMaxSize())
                        countryState.error != null -> ErrorState(
                            message = countryState.error ?: "Error",
                            modifier = Modifier.fillMaxSize()
                        )
                        countryState.countries.isEmpty() -> EmptyState(Modifier.fillMaxSize())
                        else -> {
                            val contentPadding = PaddingValues(
                                horizontal = if (isExpanded) 24.dp else 16.dp,
                                vertical = 12.dp
                            )

                            if (isExpanded) {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 320.dp),
                                    state = gridState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = contentPadding,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(countryState.countries) { country ->
                                        CountryCard(country = country, isCompact = false, onEnsureBackImage = countryViewModel::ensureBackImage)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = contentPadding,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(countryState.countries) { country ->
                                        CountryCard(country = country, isCompact = true, onEnsureBackImage = countryViewModel::ensureBackImage)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(48.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        Text(
            "Cargando...",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ErrorState(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No hay países en caché todavía.",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun CountryCard(
    country: Country,
    isCompact: Boolean,
    onEnsureBackImage: (String) -> Unit,
) {
    var expanded by remember(country.code) { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    val shape = RoundedCornerShape(20.dp)
    val backImageHeight = if (isCompact) 160.dp else 200.dp

    Card(
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable {
                val next = !expanded
                expanded = next
                if (next && country.backUrl.isNullOrBlank()) {
                    onEnsureBackImage(country.code)
                }
                runCatching { haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove) }
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        AnimatedContent(
            targetState = expanded,
            transitionSpec = {
                (fadeIn(tween(180)) + expandVertically(tween(220)))
                    .togetherWith(fadeOut(tween(120)) + shrinkVertically(tween(180)))
                    .using(SizeTransform(clip = false))
            },
            label = "CountryCardExpand"
        ) { isExpanded ->
            if (!isExpanded) {
                CollapsedCardContent(
                    country = country,
                    shape = shape,
                )
            } else {
                ExpandedCardContent(
                    country = country,
                    shape = shape,
                    backImageHeight = backImageHeight,
                )
            }
        }
    }
}

@Composable
private fun CollapsedCardContent(
    country: Country,
    shape: RoundedCornerShape,
) {
    val flagUrl = country.flagUrl

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 110.dp)
    ) {
        if (!flagUrl.isNullOrBlank()) {
            KamelImage(
                resource = { asyncPainterResource(flagUrl) },
                contentDescription = "Flag ${country.name}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(shape),
                onLoading = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(strokeWidth = 2.dp) }
                },
                onFailure = { exception ->
                    runCatching {
                        object {}.logE(
                            "Flag image failed for ${country.code}: ${exception.message} url=$flagUrl"
                        )
                    }.getOrNull()

                    // Fallback visible si la imagen falla (en vez de negro)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.secondaryContainer
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sin imagen",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                animationSpec = tween(250)
            )
        } else {
            // Fallback bonito si no hay bandera
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    )
            )
        }

        // Overlay para legibilidad + info mínima
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xAA000000))
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = country.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = country.continent.ifBlank { "Continente" },
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExpandedCardContent(
    country: Country,
    shape: RoundedCornerShape,
    backImageHeight: Dp,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val backUrl = country.backUrl
        if (!backUrl.isNullOrBlank()) {
            KamelImage(
                resource = { asyncPainterResource(backUrl) },
                contentDescription = "Back ${country.name}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(backImageHeight),
                onLoading = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(backImageHeight),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(strokeWidth = 2.dp) }
                },
                onFailure = { exception ->
                    runCatching {
                        object {}.logE(
                            "Back image failed for ${country.code}: ${exception.message} url=$backUrl"
                        )
                    }.getOrNull()

                    // Fallback si falla el backUrl
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(backImageHeight)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.secondaryContainer
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Imagen no disponible",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                animationSpec = tween(250)
            )
        } else {
            // Si no hay backUrl, evitamos usar la bandera como header para no confundir.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(backImageHeight)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    )
            ) {
                Text(
                    text = country.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = country.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = Int.MAX_VALUE,
                softWrap = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            text = country.continent.ifBlank { "Continente" },
                            maxLines = 2,
                            softWrap = true
                        )
                    }
                )

                if (country.capital.isNotBlank()) {
                    Text(
                        text = "Capital: ${country.capital}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = Int.MAX_VALUE,
                        softWrap = true
                    )
                }
            }

            if (!country.flagUrl.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KamelImage(
                        resource = { asyncPainterResource(country.flagUrl) },
                        contentDescription = "Flag ${country.name}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 64.dp, height = 40.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        animationSpec = tween(250)
                    )
                    Text(
                        text = country.code,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

            }
        }
    }
}
