package es.sebas1705.axiomnode.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.sebas1705.axiomnode.domain.models.Game
import es.sebas1705.axiomnode.domain.models.GameStats
import es.sebas1705.axiomnode.domain.usecases.GamesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeState(
    val isLoading: Boolean = true,
    val featuredGames: List<Game> = emptyList(),
    val stats: GameStats? = null,
    val pendingSyncCount: Int = 0,
    val error: String? = null,
)

/**
 * Aggregates the data shown on the dashboard / Home screen:
 * featured (random) games, aggregated stats, and pending sync count.
 */
class HomeViewModel(
    private val gamesUseCase: GamesUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val gamesRes = gamesUseCase.getRandomGames(count = 3)
            val statsRes = gamesUseCase.getGameStats()
            val pending = gamesUseCase.getPendingSyncCount()
            _state.value = HomeState(
                isLoading = false,
                featuredGames = gamesRes.getOrNull().orEmpty(),
                stats = statsRes.getOrNull(),
                pendingSyncCount = pending,
                error = listOfNotNull(
                    gamesRes.exceptionOrNull()?.message,
                    statsRes.exceptionOrNull()?.message,
                ).firstOrNull(),
            )
        }
    }

    fun syncPending(onResult: (String) -> Unit) {
        viewModelScope.launch {
            gamesUseCase.syncPendingResults()
                .onSuccess { count ->
                    _state.value = _state.value.copy(
                        pendingSyncCount = gamesUseCase.getPendingSyncCount(),
                    )
                    onResult("$count resultado${if (count != 1) "s" else ""} sincronizado${if (count != 1) "s" else ""}")
                }
                .onFailure { onResult("Error al sincronizar: ${it.message}") }
        }
    }
}

