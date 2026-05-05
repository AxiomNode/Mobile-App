package es.sebas1705.axiomnode.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.sebas1705.axiomnode.domain.models.GameResult
import es.sebas1705.axiomnode.domain.usecases.GamesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HistoryState(
    val isLoading: Boolean = true,
    val results: List<GameResult> = emptyList(),
    val pendingSyncCount: Int = 0,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val error: String? = null,
)

class HistoryViewModel(
    private val gamesUseCase: GamesUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(HistoryState())
    val state: StateFlow<HistoryState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            gamesUseCase.getRecentResults(50)
                .onSuccess { results ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        results = results,
                        pendingSyncCount = gamesUseCase.getPendingSyncCount(),
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Error al cargar el historial",
                    )
                }
        }
    }

    fun syncPending() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSyncing = true, syncMessage = null)
            gamesUseCase.syncPendingResults()
                .onSuccess { count ->
                    _state.value = _state.value.copy(
                        isSyncing = false,
                        syncMessage = "$count sincronizado${if (count != 1) "s" else ""}",
                        pendingSyncCount = gamesUseCase.getPendingSyncCount(),
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isSyncing = false,
                        syncMessage = "Error: ${e.message}",
                    )
                }
        }
    }
}

