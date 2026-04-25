package es.sebas1705.axiomnode.presentation.lobby

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.sebas1705.axiomnode.data.network.ConnectivityMonitor
import es.sebas1705.axiomnode.domain.models.GameType
import es.sebas1705.axiomnode.presentation.games.GamesViewModel
import es.sebas1705.axiomnode.presentation.navigation.Destination
import es.sebas1705.axiomnode.presentation.navigation.Navigator
import es.sebas1705.axiomnode.ui.components.AppScaffold
import es.sebas1705.axiomnode.ui.components.ErrorState
import es.sebas1705.axiomnode.ui.components.InlineErrorCard
import es.sebas1705.axiomnode.ui.components.InlineInfoCard
import es.sebas1705.axiomnode.ui.components.LoadingState
import es.sebas1705.axiomnode.ui.components.SectionHeader
import es.sebas1705.axiomnode.ui.layout.LocalWindowSize
import es.sebas1705.axiomnode.ui.layout.WindowSize
import org.koin.compose.koinInject

/**
 * Configurable lobby screen used to generate a brand-new Quiz or Wordpass game.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameLobbyScreen(
    gameType: GameType,
    viewModel: GamesViewModel,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val connectivityMonitor: ConnectivityMonitor = koinInject()
    val isOnline by connectivityMonitor.isOnline.collectAsStateWithLifecycle()

    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var language by rememberSaveable { mutableStateOf("es") }
    var difficulty by rememberSaveable { mutableStateOf(50f) }
    var numQuestions by rememberSaveable { mutableStateOf(10) }
    var letters by rememberSaveable { mutableStateOf("") }
    var languageMenuOpen by remember { mutableStateOf(false) }
    val windowSize = LocalWindowSize.current
    val horizontalGutter = when (windowSize) {
        WindowSize.COMPACT -> 16.dp
        WindowSize.MEDIUM -> 20.dp
        WindowSize.EXPANDED -> 24.dp
    }

    val title = when (gameType) {
        GameType.QUIZ -> "Configurar Quiz"
        GameType.WORDPASS -> "Configurar Wordpass"
    }

    // Cuando se genera un nuevo juego, ir a Play.
    LaunchedEffect(state.lastGeneratedGameId) {
        val generatedId = state.lastGeneratedGameId ?: return@LaunchedEffect
        navigator.push(Destination.Play(generatedId))
        viewModel.consumeGeneratedNavigation()
    }

    // Al entrar al lobby intentamos traer contenido nuevo (si hay red) para mejorar variedad.
    LaunchedEffect(gameType, isOnline) {
        if (isOnline) {
            viewModel.warmUpPlayContent(count = 6)
        }
    }

    // Ensure catalog is requested when entering the lobby (useful after transient STG failures).
    LaunchedEffect(state.catalog, state.isLoading) {
        if (state.catalog == null && !state.isLoading) {
            viewModel.loadCatalog()
        }
    }

    AppScaffold(
        title = title,
        modifier = modifier,
        onBack = { navigator.pop() },
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalGutter),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Categorías
            SectionHeader(title = "Categoría")
            val catalog = state.catalog
            when {
                state.isLoading && catalog == null -> {
                    LoadingState(
                        message = "Cargando categorías…",
                        modifier = Modifier.height(120.dp),
                    )
                }
                catalog == null && state.error != null -> {
                    ErrorState(
                        message = state.error ?: "No se pudo cargar el catálogo",
                        modifier = Modifier.height(180.dp),
                        onRetry = { viewModel.loadCatalog() },
                    )
                }
                catalog == null -> {
                    LoadingState(
                        message = "Preparando catálogo…",
                        modifier = Modifier.height(120.dp),
                    )
                }
                else -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        catalog.categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat.id,
                                onClick = { selectedCategory = cat.id },
                                label = { Text(cat.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                            )
                        }
                    }
                }
            }

            // ── Idioma
            SectionHeader(title = "Idioma")
            val languages = catalog?.languages.orEmpty()
            ExposedDropdownMenuBox(
                expanded = languageMenuOpen,
                onExpandedChange = { languageMenuOpen = it },
            ) {
                OutlinedTextField(
                    value = languages.firstOrNull { it.code == language }?.name ?: language.uppercase(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Idioma") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageMenuOpen) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                    shape = RoundedCornerShape(12.dp),
                )
                DropdownMenu(
                    expanded = languageMenuOpen,
                    onDismissRequest = { languageMenuOpen = false },
                ) {
                    languages.forEach { lang ->
                        DropdownMenuItem(
                            text = { Text(lang.name) },
                            onClick = {
                                language = lang.code
                                viewModel.setSelectedLanguage(lang.code)
                                languageMenuOpen = false
                            },
                        )
                    }
                }
            }

            // ── Nº de preguntas
            SectionHeader(title = "Número de preguntas")
            val options = listOf(5, 10, 15, 20)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, value ->
                    SegmentedButton(
                        selected = numQuestions == value,
                        onClick = { numQuestions = value },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    ) { Text("$value") }
                }
            }

            // ── Dificultad
            SectionHeader(title = "Dificultad: ${difficulty.toInt()}%")
            Slider(
                value = difficulty,
                onValueChange = { difficulty = it },
                valueRange = 0f..100f,
                steps = 9,
                modifier = Modifier.fillMaxWidth(),
            )

            // ── Letras (sólo Wordpass)
            if (gameType == GameType.WORDPASS) {
                SectionHeader(title = "Letras (opcional)")
                OutlinedTextField(
                    value = letters,
                    onValueChange = { letters = it.uppercase() },
                    placeholder = { Text("Ej. ABCDE") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
            }

            state.error?.let { InlineErrorCard(message = it) }

            if (!isOnline) {
                InlineInfoCard(
                    message = "Modo offline: usarás contenido jugado/cacheado hasta recuperar conexión.",
                )
            }

            // ── CTA
            Button(
                onClick = {
                    val cat = selectedCategory ?: catalog?.categories?.firstOrNull()?.id ?: return@Button
                    val selectedNumQuestions = numQuestions
                    val selectedDifficulty = difficulty.toInt()
                    when (gameType) {
                        GameType.QUIZ -> viewModel.generateQuizGame(
                            categoryId = cat,
                            language = language,
                            numQuestions = selectedNumQuestions,
                            difficultyPercentage = selectedDifficulty,
                        )
                        GameType.WORDPASS -> viewModel.generateWordpassGame(
                            categoryId = cat,
                            language = language,
                            letters = letters.ifBlank { null },
                            numQuestions = selectedNumQuestions,
                            difficultyPercentage = selectedDifficulty,
                        )
                    }
                },
                enabled = !state.isLoading && state.catalog != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(if (state.isLoading) "Generando…" else "Empezar partida",
                    style = MaterialTheme.typography.labelLarge)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
