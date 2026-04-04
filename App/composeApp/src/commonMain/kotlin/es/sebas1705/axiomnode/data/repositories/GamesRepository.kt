package es.sebas1705.axiomnode.data.repositories

import es.sebas1705.axiomnode.data.db.GameDao
import es.sebas1705.axiomnode.data.db.GameResultDao
import es.sebas1705.axiomnode.data.entities.GameEntity
import es.sebas1705.axiomnode.data.entities.GameResultEntity
import es.sebas1705.axiomnode.data.network.GameGenerateRequest
import es.sebas1705.axiomnode.data.network.GamesHttpClient
import es.sebas1705.axiomnode.domain.models.Game
import es.sebas1705.axiomnode.domain.models.GameCatalog
import es.sebas1705.axiomnode.domain.models.GameResult
import es.sebas1705.axiomnode.domain.usecases.GamesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * Implementacion del caso de uso de juegos con cache local.
 */
class GamesRepository(
    private val httpClient: GamesHttpClient,
    private val gameDao: GameDao,
    private val gameResultDao: GameResultDao,
    private val authToken: String = "",
) : GamesUseCase {
    override suspend fun getGameCatalog(): Result<GameCatalog> {
        return httpClient.getGameCatalog()
    }

    override suspend fun generateGame(
        categoryId: String,
        language: String,
        numQuestions: Int,
        difficultyPercentage: Int,
    ): Result<Game> = withContext(Dispatchers.IO) {
        val request = GameGenerateRequest(
            language = language,
            categoryId = categoryId,
            numQuestions = numQuestions,
            difficultyPercentage = difficultyPercentage,
        )
        httpClient.generateGame(request, authToken)
            .onSuccess { game ->
                gameDao.insertGame(GameEntity.fromDomain(game))
            }
    }

    override suspend fun getRandomGames(
        count: Int,
        language: String,
        categoryId: String?,
    ): Result<List<Game>> = withContext(Dispatchers.IO) {
        httpClient.getRandomGames(count, language, categoryId)
            .onSuccess { games ->
                gameDao.insertGames(games.map { GameEntity.fromDomain(it) })
            }
    }

    override suspend fun recordGameResult(result: GameResult): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = GameResultEntity.fromDomain(result)
            gameResultDao.insertResult(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

