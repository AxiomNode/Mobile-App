package es.sebas1705.axiomnode.presentation.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.sebas1705.axiomnode.domain.models.Game
import es.sebas1705.axiomnode.domain.models.GameCatalog
import es.sebas1705.axiomnode.domain.models.GameType
import es.sebas1705.axiomnode.domain.usecases.GamesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GamesState(
    val isLoading: Boolean = false,
    val catalog: GameCatalog? = null,
    val games: List<Game> = emptyList(),
    val error: String? = null,
    val contentAdvice: String? = null,
    val selectedCategoryId: String? = null,
    val selectedLanguage: String = "es",
)

class GamesViewModel(
    private val gamesUseCase: GamesUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(GamesState())
    val state: StateFlow<GamesState> = _state.asStateFlow()

    init {
        loadCatalog()
    }

    fun loadCatalog() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            gamesUseCase.getGameCatalog()
                .onSuccess { catalog ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        catalog = catalog,
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Error al cargar catalogo",
                    )
                }
        }
    }

    fun loadRandomGames(count: Int = 5) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            gamesUseCase.getRandomGames(
                count = count,
                language = _state.value.selectedLanguage,
                categoryId = _state.value.selectedCategoryId,
            )
                .onSuccess { games ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        games = games,
                        contentAdvice = buildContentAdvice(games, requestedCount = count),
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Error al cargar juegos",
                    )
                }
        }
    }

    fun resolveGameForPlay(gameId: String, onResolved: (Game?) -> Unit) {
        viewModelScope.launch {
            val inState = _state.value.games.firstOrNull { it.id == gameId }
            if (inState != null) {
                onResolved(inState)
                return@launch
            }

            gamesUseCase.getCachedGameById(gameId)
                .onSuccess { cached ->
                    onResolved(cached)
                    if (cached == null) {
                        _state.value = _state.value.copy(error = "No hay copia local de esta partida")
                    }
                }
                .onFailure { failure ->
                    _state.value = _state.value.copy(
                        error = failure.message ?: "No se pudo cargar la partida",
                    )
                    onResolved(null)
                }
        }
    }

    fun generateGame(categoryId: String, language: String) {
        generateGameByMode(
            categoryId = categoryId,
            language = language,
            gameType = GameType.QUIZ,
            letters = null,
        )
    }

    fun generateQuizGame(categoryId: String, language: String) {
        generateGameByMode(
            categoryId = categoryId,
            language = language,
            gameType = GameType.QUIZ,
            letters = null,
        )
    }

    fun generateWordpassGame(categoryId: String, language: String, letters: String? = null) {
        generateGameByMode(
            categoryId = categoryId,
            language = language,
            gameType = GameType.WORDPASS,
            letters = letters,
        )
    }

    private fun generateGameByMode(
        categoryId: String,
        language: String,
        gameType: GameType,
        letters: String?,
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            gamesUseCase.generateGame(
                categoryId = categoryId,
                language = language,
                numQuestions = 10,
                difficultyPercentage = 50,
                gameType = gameType,
                letters = letters,
            )
                .onSuccess { game ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        games = _state.value.games + game,
                        contentAdvice = buildContentAdvice(_state.value.games + game),
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Error al generar juego",
                    )
                }
        }
    }

    fun setSelectedCategory(categoryId: String?) {
        _state.value = _state.value.copy(selectedCategoryId = categoryId)
    }

    fun setSelectedLanguage(language: String) {
        _state.value = _state.value.copy(selectedLanguage = language)
    }

    private fun buildContentAdvice(games: List<Game>, requestedCount: Int = 0): String? {
        if (games.isEmpty()) {
            return "Sin contenido local suficiente. Conéctate a internet para descargar más partidas."
        }

        val totalQuestions = games.sumOf { it.questions.size }
        val uniqueQuestionKeys = games
            .flatMap { game ->
                game.questions.map { q ->
                    (q.text.trim() + "|" + q.correctAnswer.trim()).lowercase()
                }
            }
            .toSet()
            .size

        val repetitionRatio = if (totalQuestions > 0) {
            1f - (uniqueQuestionKeys.toFloat() / totalQuestions.toFloat())
        } else {
            1f
        }

        if (requestedCount > 0 && games.size < requestedCount) {
            return "Se encontraron menos partidas de las pedidas en caché. Conéctate para bajar más contenido."
        }
        if (totalQuestions < 12) {
            return "Contenido local limitado. Conéctate para descargar más preguntas o palabras."
        }
        if (repetitionRatio >= 0.35f) {
            return "Se detecta mucha repetición en caché. Conviene conectarse para refrescar contenido."
        }
        return null
    }
}

