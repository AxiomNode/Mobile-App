package es.sebas1705.axiomnode.domain.usecases

import es.sebas1705.axiomnode.domain.models.Game
import es.sebas1705.axiomnode.domain.models.GameCatalog
import es.sebas1705.axiomnode.domain.models.GameResult

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
    ): Result<Game>
    
    suspend fun getRandomGames(
        count: Int = 5,
        language: String = "es",
        categoryId: String? = null,
    ): Result<List<Game>>
    
    suspend fun recordGameResult(result: GameResult): Result<Unit>
}

