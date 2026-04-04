package es.sebas1705.axiomnode.presentation.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.sebas1705.axiomnode.domain.models.Game
import es.sebas1705.axiomnode.domain.models.GameCatalog
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

    fun generateGame(categoryId: String, language: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            gamesUseCase.generateGame(
                categoryId = categoryId,
                language = language,
                numQuestions = 10,
                difficultyPercentage = 50,
            )
                .onSuccess { game ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        games = _state.value.games + game,
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
}

