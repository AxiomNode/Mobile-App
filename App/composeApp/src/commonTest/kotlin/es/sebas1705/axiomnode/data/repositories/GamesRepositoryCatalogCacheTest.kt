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
import es.sebas1705.axiomnode.domain.models.GameCatalog
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock

class GamesRepositoryCatalogCacheTest {

    @Test
    fun `first install fetches remote catalog and persists it locally`() = runTest {
        var remoteCalls = 0
        val catalogDao = InMemoryCatalogDao()
        val repository = buildRepository(
            catalogDao = catalogDao,
            gamesHttpClient = buildHttpClient(
                onCatalogCall = { remoteCalls++ },
                responseBody = """
                    {
                      "categories": [{"id": "ciencia", "name": "Ciencia"}],
                      "languages": [{"code": "es", "name": "Español"}]
                    }
                """.trimIndent(),
            ),
        )

        val result = repository.getGameCatalog()

        assertTrue(result.isSuccess)
        assertEquals(1, remoteCalls)
        assertEquals(
            GameCatalog(
                categories = listOf(es.sebas1705.axiomnode.domain.models.GameCategory("ciencia", "Ciencia")),
                languages = listOf(es.sebas1705.axiomnode.domain.models.GameLanguage("es", "Español")),
            ),
            result.getOrNull(),
        )
        assertEquals(listOf(CatalogCategoryEntity("ciencia", "Ciencia")), catalogDao.getCategories())
        assertEquals(listOf(CatalogLanguageEntity("es", "Español")), catalogDao.getLanguages())
        assertNotNull(catalogDao.getSyncState())
    }

    @Test
    fun `fresh local catalog serves cache without remote call`() = runTest {
        var remoteCalls = 0
        val now = Clock.System.now().toEpochMilliseconds()
        val catalogDao = InMemoryCatalogDao().apply {
            insertCategories(listOf(CatalogCategoryEntity("math", "Matemáticas")))
            insertLanguages(listOf(CatalogLanguageEntity("es", "Español")))
            upsertSyncState(
                CatalogSyncStateEntity(
                    id = 1,
                    lastSyncAt = now,
                    catalogHash = "same-hash",
                ),
            )
        }

        val repository = buildRepository(
            catalogDao = catalogDao,
            gamesHttpClient = buildHttpClient(
                onCatalogCall = { remoteCalls++ },
                responseBody = """
                    {
                      "categories": [{"id": "remote", "name": "Remote"}],
                      "languages": [{"code": "en", "name": "English"}]
                    }
                """.trimIndent(),
            ),
        )

        val result = repository.getGameCatalog()

        assertTrue(result.isSuccess)
        assertEquals(0, remoteCalls)
        assertEquals("math", result.getOrNull()!!.categories.single().id)
        assertEquals("es", result.getOrNull()!!.languages.single().code)
    }

    @Test
    fun `stale local catalog tries remote and falls back to local when remote fails`() = runTest {
        var remoteCalls = 0
        val catalogDao = InMemoryCatalogDao().apply {
            insertCategories(listOf(CatalogCategoryEntity("history", "Historia")))
            insertLanguages(listOf(CatalogLanguageEntity("es", "Español")))
            upsertSyncState(
                CatalogSyncStateEntity(
                    id = 1,
                    lastSyncAt = 0,
                    catalogHash = "old-hash",
                ),
            )
        }

        val repository = buildRepository(
            catalogDao = catalogDao,
            gamesHttpClient = buildFailingHttpClient {
                remoteCalls++
            },
        )

        val result = repository.getGameCatalog()

        assertTrue(result.isSuccess)
        assertEquals(1, remoteCalls)
        assertEquals("history", result.getOrNull()!!.categories.single().id)
        assertEquals("es", result.getOrNull()!!.languages.single().code)
    }

    private fun buildRepository(
        catalogDao: CatalogDao,
        gamesHttpClient: GamesHttpClient,
    ): GamesRepository {
        return GamesRepository(
            httpClient = gamesHttpClient,
            gameDao = NoOpGameDao(),
            gameResultDao = NoOpGameResultDao(),
            playedGameDao = NoOpPlayedGameDao(),
            catalogDao = catalogDao,
            syncEngine = GameResultSyncEngine(
                gameResultDao = NoOpGameResultDao(),
                gamesHttpClient = gamesHttpClient,
            ),
            config = firebaseConfig(),
        )
    }

    private fun buildHttpClient(
        onCatalogCall: () -> Unit,
        responseBody: String,
    ): GamesHttpClient {
        val client = HttpClient(
            MockEngine { request ->
                if (request.url.encodedPath.endsWith("/v1/mobile/games/categories")) {
                    onCatalogCall()
                }
                respond(
                    content = responseBody,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        ) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        return GamesHttpClient(client, "https://axiomnode-gateway.amksandbox.cloud")
    }

    private fun buildFailingHttpClient(onCatalogCall: () -> Unit): GamesHttpClient {
        val client = HttpClient(
            MockEngine { request ->
                if (request.url.encodedPath.endsWith("/v1/mobile/games/categories")) {
                    onCatalogCall()
                }
                throw IllegalStateException("network down")
            },
        ) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        return GamesHttpClient(client, "https://axiomnode-gateway.amksandbox.cloud")
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

    private class InMemoryCatalogDao : CatalogDao {
        private val categories = mutableListOf<CatalogCategoryEntity>()
        private val languages = mutableListOf<CatalogLanguageEntity>()
        private var syncState: CatalogSyncStateEntity? = null

        override suspend fun getCategories(): List<CatalogCategoryEntity> = categories.sortedBy { it.name }

        override suspend fun getLanguages(): List<CatalogLanguageEntity> = languages.sortedBy { it.name }

        override suspend fun getSyncState(): CatalogSyncStateEntity? = syncState

        override suspend fun insertCategories(items: List<CatalogCategoryEntity>) {
            items.forEach { item ->
                categories.removeAll { it.id == item.id }
                categories.add(item)
            }
        }

        override suspend fun insertLanguages(items: List<CatalogLanguageEntity>) {
            items.forEach { item ->
                languages.removeAll { it.code == item.code }
                languages.add(item)
            }
        }

        override suspend fun upsertSyncState(state: CatalogSyncStateEntity) {
            syncState = state
        }

        override suspend fun clearCategories() {
            categories.clear()
        }

        override suspend fun clearLanguages() {
            languages.clear()
        }
    }

    private class NoOpGameDao : GameDao {
        override suspend fun getGameById(id: String): GameEntity? = null

        override suspend fun getRecentGames(limit: Int): List<GameEntity> = emptyList()

        override suspend fun getGamesByIds(ids: List<String>): List<GameEntity> = emptyList()

        override suspend fun insertQuizGameRow(game: QuizGameEntity) = Unit

        override suspend fun insertWordpassGameRow(game: WordpassGameEntity) = Unit

        override suspend fun clearQuizGames() = Unit

        override suspend fun clearWordpassGames() = Unit

        override suspend fun deleteOldQuizGames(olderThanMs: Long) = Unit

        override suspend fun deleteOldWordpassGames(olderThanMs: Long) = Unit

        override suspend fun insertGame(game: GameEntity) = Unit

        override suspend fun insertGames(games: List<GameEntity>) = Unit

        override suspend fun clearAllGames() = Unit

        override suspend fun deleteOldGames(olderThanMs: Long) = Unit
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

    private class NoOpPlayedGameDao : PlayedGameDao {
        override suspend fun getRecentPlayedGames(limit: Int): List<PlayedGameEntity> = emptyList()

        override suspend fun getLatestPlayedGameByGameId(gameId: String): PlayedGameEntity? = null

        override suspend fun insertPlayedGame(game: PlayedGameEntity) = Unit

        override suspend fun deleteOldPlayedGames(olderThanMs: Long) = Unit
    }
}
