package es.sebas1705.axiomnode.data.repositories

import es.sebas1705.axiomnode.config.AppConfig
import es.sebas1705.axiomnode.config.AppEnvironment
import es.sebas1705.axiomnode.data.db.CatalogDao
import es.sebas1705.axiomnode.data.db.GameDao
import es.sebas1705.axiomnode.data.db.GameResultDao
import es.sebas1705.axiomnode.data.db.PlayedGameDao
import es.sebas1705.axiomnode.data.entities.CatalogCategoryEntity
import es.sebas1705.axiomnode.data.entities.CatalogLanguageEntity
import es.sebas1705.axiomnode.data.entities.CatalogSyncStateEntity
import es.sebas1705.axiomnode.data.entities.GameEntity
import es.sebas1705.axiomnode.data.entities.GameResultEntity
import es.sebas1705.axiomnode.data.entities.PlayedGameEntity
import es.sebas1705.axiomnode.data.entities.QuizGameEntity
import es.sebas1705.axiomnode.data.entities.WordpassGameEntity
import es.sebas1705.axiomnode.data.network.GameResultSyncEngine
import es.sebas1705.axiomnode.data.network.GamesHttpClient
import es.sebas1705.axiomnode.domain.models.Game
import es.sebas1705.axiomnode.domain.models.GameType
import es.sebas1705.axiomnode.domain.models.Question
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GamesRepositoryOfflineFallbackTest {

    @Test
    fun `generateGame falls back to same type and language when exact category is missing`() = kotlinx.coroutines.test.runTest {
        val gameDao = InMemoryGameDao()
        gameDao.insertGame(
            GameEntity.fromDomain(
                sampleGame(
                    id = "quiz-local-1",
                    gameType = GameType.QUIZ,
                    categoryId = "science",
                    language = "es",
                    questionCount = 8,
                ),
            ),
        )

        val repository = buildRepository(gameDao)

        val result = repository.generateGame(
            categoryId = "history",
            language = "es",
            numQuestions = 5,
            difficultyPercentage = 50,
            gameType = GameType.QUIZ,
            letters = null,
        )

        assertTrue(result.isSuccess)
        val game = result.getOrThrow()
        assertEquals("quiz-local-1", game.id)
        assertEquals(GameType.QUIZ, game.gameType)
        assertEquals("science", game.categoryId)
        assertEquals(5, game.questions.size)
    }

    @Test
    fun `getRandomGames falls back to language pool when selected category has no local games`() = kotlinx.coroutines.test.runTest {
        val gameDao = InMemoryGameDao()
        gameDao.insertGames(
            listOf(
                GameEntity.fromDomain(
                    sampleGame(
                        id = "quiz-local-2",
                        gameType = GameType.QUIZ,
                        categoryId = "science",
                        language = "es",
                    ),
                ),
                GameEntity.fromDomain(
                    sampleGame(
                        id = "wordpass-local-2",
                        gameType = GameType.WORDPASS,
                        categoryId = "science",
                        language = "es",
                    ),
                ),
            ),
        )

        val repository = buildRepository(gameDao)

        val result = repository.getRandomGames(
            count = 2,
            language = "es",
            categoryId = "history",
        )

        assertTrue(result.isSuccess)
        val games = result.getOrThrow()
        assertEquals(2, games.size)
        assertTrue(games.all { it.language == "es" })
    }

    @Test
    fun `getRandomGames persists offline fallback batch into cache`() = kotlinx.coroutines.test.runTest {
        val gameDao = InMemoryGameDao()
        gameDao.seed(
            GameEntity.fromDomain(
                sampleGame(
                    id = "quiz-local-seed",
                    gameType = GameType.QUIZ,
                    categoryId = "science",
                    language = "es",
                ),
            ),
        )

        val repository = buildRepository(gameDao)

        val result = repository.getRandomGames(
            count = 1,
            language = "es",
            categoryId = "history",
        )

        assertTrue(result.isSuccess)
        assertEquals(1, gameDao.insertGamesCalls)
        assertEquals("quiz-local-seed", result.getOrThrow().single().id)
    }

    private fun buildRepository(gameDao: GameDao): GamesRepository {
        val client = HttpClient(
            MockEngine {
                throw IllegalStateException("offline")
            },
        )
        val httpClient = GamesHttpClient(client, "https://axiomnode-gateway.amksandbox.cloud")

        return GamesRepository(
            httpClient = httpClient,
            gameDao = gameDao,
            gameResultDao = NoOpGameResultDao(),
            playedGameDao = NoOpPlayedGameDao(),
            catalogDao = NoOpCatalogDao(),
            syncEngine = GameResultSyncEngine(
                gameResultDao = NoOpGameResultDao(),
                gamesHttpClient = httpClient,
            ),
            config = firebaseConfig(),
        )
    }

    private fun sampleGame(
        id: String,
        gameType: GameType,
        categoryId: String,
        language: String,
        questionCount: Int = 6,
    ): Game {
        return Game(
            id = id,
            gameType = gameType,
            categoryId = categoryId,
            categoryName = categoryId,
            language = language,
            questions = List(questionCount) { idx ->
                Question(
                    id = "q-${id}-$idx",
                    text = "Pregunta $idx",
                    options = listOf("A", "B"),
                    correctAnswer = "A",
                )
            },
        )
    }

    private fun firebaseConfig(): AppConfig = AppConfig(
        environment = AppEnvironment.STG,
        apiBaseUrl = "https://axiomnode-gateway.amksandbox.cloud",
        authMode = "firebase",
        firebaseApiKey = "k",
        firebaseAuthDomain = "d",
        firebaseProjectId = "p",
        firebaseStorageBucket = "s",
        firebaseMessagingSenderId = "m",
        firebaseAppId = "a",
        firebaseMeasurementId = "g",
        googleWebClientId = "w",
    )

    private class InMemoryGameDao : GameDao {
        private val items = linkedMapOf<String, GameEntity>()
        var insertGamesCalls: Int = 0

        fun seed(game: GameEntity) {
            items[game.id] = game
        }

        override suspend fun getGameById(id: String): GameEntity? = items[id]

        override suspend fun getRecentGames(limit: Int): List<GameEntity> =
            items.values
                .sortedByDescending { it.createdAt }
                .take(limit)

        override suspend fun getGamesByIds(ids: List<String>): List<GameEntity> =
            ids.mapNotNull { items[it] }

        override suspend fun insertQuizGameRow(game: QuizGameEntity) {
            items[game.id] = game.toGameEntity()
        }

        override suspend fun insertWordpassGameRow(game: WordpassGameEntity) {
            items[game.id] = game.toGameEntity()
        }

        override suspend fun clearQuizGames() {
            items.entries.removeIf { it.value.gameType.uppercase() == "QUIZ" }
        }

        override suspend fun clearWordpassGames() {
            items.entries.removeIf { it.value.gameType.uppercase() == "WORDPASS" }
        }

        override suspend fun deleteOldQuizGames(olderThanMs: Long) {
            val toDelete = items.values
                .filter { it.gameType.uppercase() == "QUIZ" && it.createdAt < olderThanMs }
                .map { it.id }
            toDelete.forEach(items::remove)
        }

        override suspend fun deleteOldWordpassGames(olderThanMs: Long) {
            val toDelete = items.values
                .filter { it.gameType.uppercase() == "WORDPASS" && it.createdAt < olderThanMs }
                .map { it.id }
            toDelete.forEach(items::remove)
        }

        override suspend fun insertGame(game: GameEntity) {
            items[game.id] = game
        }

        override suspend fun insertGames(games: List<GameEntity>) {
            insertGamesCalls++
            games.forEach { items[it.id] = it }
        }

        override suspend fun clearAllGames() {
            items.clear()
        }

        override suspend fun deleteOldGames(olderThanMs: Long) {
            val toDelete = items.values.filter { it.createdAt < olderThanMs }.map { it.id }
            toDelete.forEach(items::remove)
        }
    }

    private class NoOpPlayedGameDao : PlayedGameDao {
        override suspend fun getRecentPlayedGames(limit: Int): List<PlayedGameEntity> = emptyList()

        override suspend fun getLatestPlayedGameByGameId(gameId: String): PlayedGameEntity? = null

        override suspend fun insertPlayedGame(game: PlayedGameEntity) = Unit

        override suspend fun deleteOldPlayedGames(olderThanMs: Long) = Unit
    }

    private class NoOpGameResultDao : GameResultDao {
        override suspend fun getResultById(id: Long): GameResultEntity? = null

        override suspend fun getRecentResults(limit: Int): List<GameResultEntity> = emptyList()

        override suspend fun getUnSyncedResults(): List<GameResultEntity> = emptyList()

        override suspend fun insertResult(result: GameResultEntity) = Unit

        override suspend fun insertResults(results: List<GameResultEntity>) = Unit

        override suspend fun markAsSynced(ids: List<Long>) = Unit

        override suspend fun deleteOldResults(olderThanMs: Long) = Unit
    }

    private class NoOpCatalogDao : CatalogDao {
        override suspend fun getCategories(): List<CatalogCategoryEntity> = emptyList()

        override suspend fun getLanguages(): List<CatalogLanguageEntity> = emptyList()

        override suspend fun getSyncState(): CatalogSyncStateEntity? = null

        override suspend fun insertCategories(items: List<CatalogCategoryEntity>) = Unit

        override suspend fun insertLanguages(items: List<CatalogLanguageEntity>) = Unit

        override suspend fun upsertSyncState(state: CatalogSyncStateEntity) = Unit

        override suspend fun clearCategories() = Unit

        override suspend fun clearLanguages() = Unit
    }
}