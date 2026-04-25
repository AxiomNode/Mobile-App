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
    val selectedLanguage: String = "en",
    val lastGeneratedGameId: String? = null,
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
                    val selectedLanguage =
                        if (catalog.languages.any { it.code == _state.value.selectedLanguage }) {
                            _state.value.selectedLanguage
                        } else {
                            catalog.languages.firstOrNull()?.code ?: _state.value.selectedLanguage
                        }
                    val selectedCategory =
                        _state.value.selectedCategoryId
                            ?.takeIf { categoryId -> catalog.categories.any { it.id == categoryId } }
                    _state.value = _state.value.copy(
                        isLoading = false,
                        catalog = catalog,
                        selectedLanguage = selectedLanguage,
                        selectedCategoryId = selectedCategory,
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load catalog",
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
                        lastGeneratedGameId = null,
                        contentAdvice = buildContentAdvice(games, requestedCount = count),
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load games",
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
                        _state.value = _state.value.copy(error = "No local copy is available for this session")
                    }
                }
                .onFailure { failure ->
                    _state.value = _state.value.copy(
                        error = failure.message ?: "Failed to load session",
                    )
                    onResolved(null)
                }
        }
    }

    fun generateGame(
        categoryId: String,
        language: String,
        numQuestions: Int = 10,
        difficultyPercentage: Int = 50,
    ) {
        generateGameByMode(
            categoryId = categoryId,
            language = language,
            numQuestions = numQuestions,
            difficultyPercentage = difficultyPercentage,
            gameType = GameType.QUIZ,
            letters = null,
        )
    }

    fun generateQuizGame(
        categoryId: String,
        language: String,
        numQuestions: Int = 10,
        difficultyPercentage: Int = 50,
    ) {
        generateGameByMode(
            categoryId = categoryId,
            language = language,
            numQuestions = numQuestions,
            difficultyPercentage = difficultyPercentage,
            gameType = GameType.QUIZ,
            letters = null,
        )
    }

    fun generateWordpassGame(
        categoryId: String,
        language: String,
        letters: String? = null,
        numQuestions: Int = 10,
        difficultyPercentage: Int = 50,
    ) {
        generateGameByMode(
            categoryId = categoryId,
            language = language,
            numQuestions = numQuestions,
            difficultyPercentage = difficultyPercentage,
            gameType = GameType.WORDPASS,
            letters = letters,
        )
    }

    private fun generateGameByMode(
        categoryId: String,
        language: String,
        numQuestions: Int,
        difficultyPercentage: Int,
        gameType: GameType,
        letters: String?,
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            gamesUseCase.generateGame(
                categoryId = categoryId,
                language = language,
                numQuestions = numQuestions,
                difficultyPercentage = difficultyPercentage,
                gameType = gameType,
                letters = letters,
            )
                .onSuccess { game ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        games = _state.value.games + game,
                        lastGeneratedGameId = game.id,
                        contentAdvice = buildContentAdvice(_state.value.games + game),
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to generate game",
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

    /**
     * Silent prefetch when entering game flows: fetches fresh content online,
     * while leaving current UI stable when offline.
     */
    fun warmUpPlayContent(count: Int = 4) {
        viewModelScope.launch {
            gamesUseCase.getRandomGames(
                count = count,
                language = _state.value.selectedLanguage,
                categoryId = _state.value.selectedCategoryId,
            ).onSuccess { incoming ->
                if (incoming.isNotEmpty()) {
                    val merged = (incoming + _state.value.games)
                        .distinctBy { it.id }
                    _state.value = _state.value.copy(
                        games = merged,
                        contentAdvice = buildContentAdvice(merged),
                    )
                }
            }
        }
    }

    fun consumeGeneratedNavigation() {
        _state.value = _state.value.copy(lastGeneratedGameId = null)
    }

    private fun buildContentAdvice(games: List<Game>, requestedCount: Int = 0): String? {
        if (games.isEmpty()) {
            return "Not enough local content. Connect to the internet to download more sessions."
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
            return "Fewer cached sessions were found than requested. Connect to download more content."
        }
        if (totalQuestions < 12) {
            return "Local content is limited. Connect to download more questions or words."
        }
        if (repetitionRatio >= 0.35f) {
            return "A high amount of cached repetition was detected. Connect to refresh the content."
        }
        return null
    }
}
