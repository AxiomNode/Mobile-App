package es.sebas1705.axiomnode.data.network

import es.sebas1705.axiomnode.domain.models.Game
import es.sebas1705.axiomnode.domain.models.GameCatalog
import es.sebas1705.axiomnode.domain.models.GameCategory
import es.sebas1705.axiomnode.domain.models.GameLanguage
import es.sebas1705.axiomnode.domain.models.GameType
import es.sebas1705.axiomnode.domain.models.Question
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.random.Random

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
    val categoryName: String? = null,
    val itemCount: Int? = null,
    val numQuestions: Int = 10,
    val difficultyPercentage: Int = 50,
    val letters: String? = null,
    val requestedBy: String = "api",
)

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
        val response = httpClient.get("$baseUrl/v1/mobile/games/categories")
        val catalog = response.body<GameCatalogResponse>()
        Result.success(catalog.toDomain())
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun generateGame(
        request: GameGenerateRequest,
        authToken: String,
        gameType: GameType,
    ): Result<Game> = try {
        val endpoint = when (gameType) {
            GameType.QUIZ -> "$baseUrl/v1/mobile/games/quiz/generate"
            GameType.WORDPASS -> "$baseUrl/v1/mobile/games/wordpass/generate"
        }

        val response = httpClient.post(endpoint) {
            if (authToken.isNotBlank()) {
                bearerAuth(authToken)
            }
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        if (!response.status.isSuccess()) {
            return Result.failure(Exception("Generate failed: HTTP ${response.status.value}"))
        }

        val payload = response.body<JsonElement>()
        val game = parseGeneratedGamePayload(payload, gameType)
            ?: return Result.failure(Exception("Invalid generated game payload"))

        Result.success(game)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getRandomGames(
        count: Int = 5,
        language: String = "es",
        categoryId: String? = null,
    ): Result<List<Game>> {
        val safeCount = count.coerceAtLeast(1)
        val quizCount = (safeCount + 1) / 2
        val wordpassCount = safeCount / 2

        val quizResult = fetchRandomGamesByMode(
            mode = GameType.QUIZ,
            count = quizCount,
            language = language,
            categoryId = categoryId,
        )

        val wordpassResult = if (wordpassCount > 0) {
            fetchRandomGamesByMode(
                mode = GameType.WORDPASS,
                count = wordpassCount,
                language = language,
                categoryId = categoryId,
            )
        } else {
            Result.success(emptyList())
        }

        return when {
            quizResult.isSuccess && wordpassResult.isSuccess -> {
                val merged = interleave(
                    quizResult.getOrNull().orEmpty(),
                    wordpassResult.getOrNull().orEmpty(),
                ).take(safeCount)
                Result.success(merged)
            }

            quizResult.isSuccess -> Result.success(quizResult.getOrNull().orEmpty().take(safeCount))
            wordpassResult.isSuccess -> Result.success(wordpassResult.getOrNull().orEmpty().take(safeCount))

            else -> {
                val quizError = quizResult.exceptionOrNull()?.message ?: "unknown quiz error"
                val wordpassError = wordpassResult.exceptionOrNull()?.message ?: "unknown wordpass error"
                Result.failure(Exception("Random games failed. quiz=$quizError, wordpass=$wordpassError"))
            }
        }
    }

    /**
     * Sync game results to backend.
     * Route: POST /mobile/games/events
     */
    suspend fun syncGameResults(
        events: List<GameEventDto>,
        authToken: String,
    ): Result<SyncResponse> = try {
        val response = httpClient.post("$baseUrl/v1/mobile/games/events") {
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

    private suspend fun fetchRandomGamesByMode(
        mode: GameType,
        count: Int,
        language: String,
        categoryId: String?,
    ): Result<List<Game>> = try {
        val endpoint = when (mode) {
            GameType.QUIZ -> "$baseUrl/v1/mobile/games/quiz/random"
            GameType.WORDPASS -> "$baseUrl/v1/mobile/games/wordpass/random"
        }

        val response = httpClient.get(endpoint) {
            parameter("count", count)
            parameter("language", language)
            categoryId?.let { parameter("categoryId", it) }
        }

        if (!response.status.isSuccess()) {
            return Result.failure(Exception("Random ${mode.name.lowercase()} failed: HTTP ${response.status.value}"))
        }

        val payload = response.body<JsonElement>()
        Result.success(parseRandomGamesPayload(payload, mode))
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun parseGeneratedGamePayload(payload: JsonElement, fallbackType: GameType): Game? {
        if (payload !is JsonObject) return null

        val envelopeType = payload.stringOrNull("gameType")
        val resolvedType = parseGameType(envelopeType, fallbackType)
        val generated = payload.jsonObjectOrNull("generated") ?: payload
        return parseGameFromModel(generated, resolvedType)
    }

    private fun parseRandomGamesPayload(payload: JsonElement, fallbackType: GameType): List<Game> {
        return when (payload) {
            is JsonArray -> payload.mapNotNull { element ->
                val model = element as? JsonObject ?: return@mapNotNull null
                val type = parseGameType(model.stringOrNull("gameType"), fallbackType)
                parseGameFromModel(model, type)
            }

            is JsonObject -> {
                val envelopeType = parseGameType(payload.stringOrNull("gameType"), fallbackType)
                val items = payload.jsonArrayOrNull("items")
                if (items != null) {
                    items.mapNotNull { element ->
                        val model = element as? JsonObject ?: return@mapNotNull null
                        parseGameFromModel(model, envelopeType)
                    }
                } else {
                    listOfNotNull(parseGameFromModel(payload, envelopeType))
                }
            }

            else -> emptyList()
        }
    }

    private fun parseGameFromModel(model: JsonObject, fallbackType: GameType): Game? {
        val game = model.jsonObjectOrNull("game") ?: model
        val resolvedType = parseGameType(
            game.stringOrNull("gameType", "game_type") ?: model.stringOrNull("gameType", "game_type"),
            fallbackType,
        )

        val id = game.stringOrNull("id", "gameId", "game_id", "modelId")
            ?: "generated-${resolvedType.name.lowercase()}-${Random.nextInt(100000, 999999)}"
        val categoryId = game.stringOrNull("categoryId", "category_id") ?: "general"
        val categoryName = game.stringOrNull("categoryName", "category_name") ?: categoryId
        val language = game.stringOrNull("language", "lang") ?: "es"

        val questions = when (resolvedType) {
            GameType.QUIZ -> parseQuizQuestions(game)
            GameType.WORDPASS -> parseWordpassQuestions(game)
        }

        if (questions.isEmpty()) return null

        return Game(
            id = id,
            gameType = resolvedType,
            categoryId = categoryId,
            categoryName = categoryName,
            language = language,
            questions = questions,
        )
    }

    private fun parseQuizQuestions(game: JsonObject): List<Question> {
        val source = game.jsonArrayOrNull("questions") ?: return emptyList()
        return source.mapIndexedNotNull { index, element ->
            val obj = element as? JsonObject ?: return@mapIndexedNotNull null
            val text = obj.stringOrNull("text", "question", "prompt") ?: return@mapIndexedNotNull null
            val options = obj.stringListOrNull("options", "choices").orEmpty()
            if (options.isEmpty()) return@mapIndexedNotNull null

            val explicit = obj.stringOrNull("correctAnswer", "correct_answer", "answer")
            val correctIndex = obj.intOrNull("correct_index", "correctIndex")
            val byIndex = correctIndex?.let { idx -> options.getOrNull(idx) }
            val correctAnswer = explicit ?: byIndex ?: return@mapIndexedNotNull null

            Question(
                id = obj.stringOrNull("id", "questionId") ?: "q-${index + 1}",
                text = text,
                options = options,
                correctAnswer = correctAnswer,
            )
        }
    }

    private fun parseWordpassQuestions(game: JsonObject): List<Question> {
        val source = game.jsonArrayOrNull("words") ?: return emptyList()
        return source.mapIndexedNotNull { index, element ->
            val obj = element as? JsonObject ?: return@mapIndexedNotNull null
            val answer = obj.stringOrNull("answer", "word", "solution") ?: return@mapIndexedNotNull null
            val clue = obj.stringOrNull("hint", "clue", "question", "definition", "letter")
                ?: "Palabra ${index + 1}"

            Question(
                id = obj.stringOrNull("id", "wordId") ?: "w-${index + 1}",
                text = clue,
                options = emptyList(),
                correctAnswer = answer,
            )
        }
    }

    private fun parseGameType(raw: String?, fallback: GameType): GameType {
        val normalized = raw?.trim()?.lowercase()
        return when (normalized) {
            "quiz" -> GameType.QUIZ
            "word-pass", "wordpass", "word_pass" -> GameType.WORDPASS
            else -> fallback
        }
    }

    private fun interleave(first: List<Game>, second: List<Game>): List<Game> {
        val merged = mutableListOf<Game>()
        val maxSize = maxOf(first.size, second.size)
        repeat(maxSize) { idx ->
            first.getOrNull(idx)?.let(merged::add)
            second.getOrNull(idx)?.let(merged::add)
        }
        return merged
    }

    private fun JsonObject.jsonObjectOrNull(key: String): JsonObject? =
        (this[key] as? JsonObject)

    private fun JsonObject.jsonArrayOrNull(key: String): JsonArray? =
        (this[key] as? JsonArray)

    private fun JsonObject.stringOrNull(vararg keys: String): String? {
        for (key in keys) {
            val primitive = this[key]?.jsonPrimitive ?: continue
            val candidate = runCatching { primitive.content }.getOrNull()
            if (!candidate.isNullOrBlank()) return candidate
        }
        return null
    }

    private fun JsonObject.intOrNull(vararg keys: String): Int? {
        for (key in keys) {
            val primitive = this[key]?.jsonPrimitive ?: continue
            val value = runCatching { primitive.content.toInt() }.getOrNull()
            if (value != null) return value
        }
        return null
    }

    private fun JsonObject.stringListOrNull(vararg keys: String): List<String>? {
        for (key in keys) {
            val array = this[key] as? JsonArray ?: continue
            val values = array.mapNotNull { element ->
                val value = runCatching { element.jsonPrimitive.content }.getOrNull()
                value?.takeIf { it.isNotBlank() }
            }
            if (values.isNotEmpty()) return values
        }
        return null
    }
}
