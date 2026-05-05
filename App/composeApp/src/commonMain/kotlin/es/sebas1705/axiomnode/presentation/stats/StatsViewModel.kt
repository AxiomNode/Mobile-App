package es.sebas1705.axiomnode.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.sebas1705.axiomnode.domain.models.GameResult
import es.sebas1705.axiomnode.domain.models.GameStats
import es.sebas1705.axiomnode.domain.models.GameType
import es.sebas1705.axiomnode.domain.usecases.GamesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StatsState(
    val isLoading: Boolean = true,
    val global: GameStats? = null,
    val byType: Map<GameType, TypeStats> = emptyMap(),
    val byCategory: List<CategoryStats> = emptyList(),
    val error: String? = null,
)

data class TypeStats(
    val totalGames: Int,
    val wins: Int,
    val averageScore: Int,
)

data class CategoryStats(
    val categoryName: String,
    val totalGames: Int,
    val wins: Int,
    val averageScore: Int,
)

class StatsViewModel(
    private val gamesUseCase: GamesUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(StatsState())
    val state: StateFlow<StatsState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val stats = gamesUseCase.getGameStats().getOrNull()
            val recent = gamesUseCase.getRecentResults(200).getOrNull().orEmpty()
            _state.value = StatsState(
                isLoading = false,
                global = stats,
                byType = computeByType(recent),
                byCategory = computeByCategory(recent),
            )
        }
    }

    private fun computeByType(results: List<GameResult>): Map<GameType, TypeStats> =
        results.groupBy { it.gameType }
            .mapValues { (_, list) ->
                TypeStats(
                    totalGames = list.size,
                    wins = list.count { it.outcome.name == "WON" },
                    averageScore = if (list.isNotEmpty()) list.sumOf { it.score } / list.size else 0,
                )
            }

    private fun computeByCategory(results: List<GameResult>): List<CategoryStats> =
        results.groupBy { it.categoryId to it.categoryName }
            .map { (key, list) ->
                CategoryStats(
                    categoryName = key.second,
                    totalGames = list.size,
                    wins = list.count { it.outcome.name == "WON" },
                    averageScore = if (list.isNotEmpty()) list.sumOf { it.score } / list.size else 0,
                )
            }
            .sortedByDescending { it.totalGames }
}

