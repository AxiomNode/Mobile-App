package es.sebas1705.axiomnode.domain.usecases

import es.sebas1705.axiomnode.domain.models.Game
import es.sebas1705.axiomnode.domain.models.GameCatalog
import es.sebas1705.axiomnode.domain.models.GameResult
import es.sebas1705.axiomnode.domain.models.GameStats
import es.sebas1705.axiomnode.domain.models.GameType

/**
 * Casos de uso para juegos (Quiz y Wordpass).
 */
interface GamesUseCase {
    suspend fun getGameCatalog(): Result<GameCatalog>
    
    suspend fun generateGame(
        categoryId: String,
        language: String,
        numQuestions: Int = 10,
        difficultyPercentage: Int = 50,
        gameType: GameType = GameType.QUIZ,
        letters: String? = null,
    ): Result<Game>
    
    suspend fun getRandomGames(
        count: Int = 5,
        language: String = "es",
        categoryId: String? = null,
    ): Result<List<Game>>
    
    suspend fun recordGameResult(result: GameResult): Result<Unit>

    suspend fun getRecentResults(limit: Int = 20): Result<List<GameResult>>

    suspend fun getGameStats(): Result<GameStats>

    /**
     * Sync all unsynced game results to the backend.
     * @return number of results successfully synced.
     */
    suspend fun syncPendingResults(): Result<Int>

    /**
     * Returns the count of results pending sync.
     */
    suspend fun getPendingSyncCount(): Int
}

