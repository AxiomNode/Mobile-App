package es.sebas1705.axiomnode.data.network

import es.sebas1705.axiomnode.domain.models.Game
import es.sebas1705.axiomnode.domain.models.GameCatalog
import es.sebas1705.axiomnode.domain.models.GameCategory
import es.sebas1705.axiomnode.domain.models.GameLanguage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

@Serializable
data class GameCatalogResponse(
    val categories: List<CategoryDto>,
    val languages: List<LanguageDto>,
) {
    fun toDomain(): GameCatalog = GameCatalog(
        categories = categories.map { GameCategory(it.id, it.name) },
        languages = languages.map { GameLanguage(it.code, it.name) },
    )
}

@Serializable
data class CategoryDto(val id: String, val name: String)

@Serializable
data class LanguageDto(val code: String, val name: String)

@Serializable
data class GameGenerateRequest(
    val language: String,
    val categoryId: String,
    val numQuestions: Int = 10,
    val difficultyPercentage: Int = 50,
)

@Serializable
data class QuestionDto(
    val id: String,
    val text: String,
    val options: List<String>,
    val correctAnswer: String,
)

@Serializable
data class GameResponse(
    val id: String,
    val gameType: String, // QUIZ, WORDPASS
    val categoryId: String,
    val categoryName: String,
    val language: String,
    val questions: List<QuestionDto>,
) {
    fun toDomain(): Game = Game(
        id = id,
        gameType = es.sebas1705.axiomnode.domain.models.GameType.valueOf(gameType),
        categoryId = categoryId,
        categoryName = categoryName,
        language = language,
        questions = questions.map {
            es.sebas1705.axiomnode.domain.models.Question(
                it.id, it.text, it.options, it.correctAnswer,
            )
        },
    )
}

/**
 * Request body for syncing game results to backend.
 * Route: POST /mobile/games/events
 * (api-gateway → bff-mobile → microservice-users /users/me/games/events)
 */
@Serializable
data class GameResultSyncRequest(
    val events: List<GameEventDto>,
)

@Serializable
data class GameEventDto(
    val gameId: String,
    val gameType: String,
    val categoryId: String,
    val categoryName: String,
    val language: String,
    val outcome: String,
    val score: Int,
    val durationSeconds: Int,
    val timestamp: Long,
)

@Serializable
data class SyncResponse(
    val synced: Int? = null,
    val message: String? = null,
)

/**
 * HTTP client for game operations via api-gateway → bff-mobile → microservice-quizz/wordpass.
 *
 * @param baseUrl The api-gateway edge URL (e.g. http://localhost:7005).
 */
class GamesHttpClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) {
    suspend fun getGameCatalog(): Result<GameCatalog> = try {
        val response = httpClient.get("$baseUrl/mobile/games/categories")
        val catalog = response.body<GameCatalogResponse>()
        Result.success(catalog.toDomain())
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun generateGame(request: GameGenerateRequest, authToken: String): Result<Game> = try {
        val response = httpClient.post("$baseUrl/mobile/games/generate") {
            bearerAuth(authToken)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        val game = response.body<GameResponse>()
        Result.success(game.toDomain())
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getRandomGames(
        count: Int = 5,
        language: String = "es",
        categoryId: String? = null,
    ): Result<List<Game>> = try {
        val response = httpClient.get("$baseUrl/mobile/games/models/random") {
            parameter("count", count)
            parameter("language", language)
            categoryId?.let { parameter("categoryId", it) }
        }
        val games = response.body<List<GameResponse>>()
        Result.success(games.map { it.toDomain() })
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Sync game results to backend.
     * Route: POST /mobile/games/events
     */
    suspend fun syncGameResults(
        events: List<GameEventDto>,
        authToken: String,
    ): Result<SyncResponse> = try {
        val response = httpClient.post("$baseUrl/mobile/games/events") {
            bearerAuth(authToken)
            contentType(ContentType.Application.Json)
            setBody(GameResultSyncRequest(events))
        }
        if (response.status.isSuccess()) {
            val body = response.body<SyncResponse>()
            Result.success(body)
        } else {
            Result.failure(Exception("Sync failed: HTTP ${response.status.value}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
