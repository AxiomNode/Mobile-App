package es.sebas1705.axiomnode.data.network

import es.sebas1705.axiomnode.domain.models.GameType
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GamesHttpClientRoutesTest {

    @Test
    fun `get catalog uses categories endpoint`() {
        var capturedPath = ""
        val http = HttpClient(
            MockEngine { request ->
                capturedPath = request.url.encodedPath
                respond(
                    content =
                        """
                            {
                              "categories": [{"id": "ciencia", "name": "Ciencia"}],
                              "languages": [{"code": "es", "name": "Español"}]
                            }
                        """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        ) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val client = GamesHttpClient(http, "https://axiomnode-gateway.amksandbox.cloud")

        val result = kotlinx.coroutines.runBlocking { client.getGameCatalog() }

        assertTrue(result.isSuccess)
        assertEquals("/v1/mobile/games/categories", capturedPath)
        assertEquals(1, result.getOrThrow().categories.size)
        assertEquals(1, result.getOrThrow().languages.size)
    }

    @Test
    fun `generate quiz uses quiz endpoint`() {
        var capturedPath = ""
        val http = HttpClient(
            MockEngine { request ->
                capturedPath = request.url.encodedPath
                respond(
                    content = """
                        {
                          "gameType": "quiz",
                          "generated": {
                            "id": "quiz-1",
                            "categoryId": "ciencia",
                            "categoryName": "Ciencia",
                            "language": "es",
                            "questions": [
                              {"id":"q1","question":"2+2","answers":["4","5"],"correctIndex":0}
                            ]
                          }
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        ) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val client = GamesHttpClient(http, "https://axiomnode-gateway.amksandbox.cloud")

        val result =
            kotlinx.coroutines.runBlocking {
                client.generateGame(
                    request = GameGenerateRequest(language = "es", categoryId = "ciencia"),
                    authToken = "token",
                    gameType = GameType.QUIZ,
                )
            }

        assertTrue(result.isSuccess)
        assertEquals("/v1/mobile/games/quiz/generate", capturedPath)
    }

    @Test
    fun `generate wordpass uses wordpass endpoint`() {
        var capturedPath = ""
        val http = HttpClient(
            MockEngine { request ->
                capturedPath = request.url.encodedPath
                respond(
                    content = """
                        {
                          "gameType": "wordpass",
                          "generated": {
                            "id": "wp-1",
                            "categoryId": "historia",
                            "categoryName": "Historia",
                            "language": "es",
                            "words": [
                              {"id":"w1","definition":"Capital de Francia","word":"Paris"}
                            ]
                          }
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        ) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val client = GamesHttpClient(http, "https://axiomnode-gateway.amksandbox.cloud")

        val result =
            kotlinx.coroutines.runBlocking {
                client.generateGame(
                    request = GameGenerateRequest(language = "es", categoryId = "historia"),
                    authToken = "token",
                    gameType = GameType.WORDPASS,
                )
            }

        assertTrue(result.isSuccess)
        assertEquals("/v1/mobile/games/wordpass/generate", capturedPath)
    }

    @Test
    fun `generate quiz parses stored model envelope with response object`() {
        val http = HttpClient(
            MockEngine {
                respond(
                    content = """
                        {
                          "gameType": "quiz",
                          "generated": {
                            "id": "quiz-model-1",
                            "gameType": "quiz",
                            "categoryId": "ciencia",
                            "categoryName": "Ciencia",
                            "language": "es",
                            "request": {"categoryId": "ciencia", "language": "es"},
                            "response": {
                              "questions": [
                                {"id":"q1","question":"2+2","answers":["4","5"],"correctIndex":0}
                              ]
                            }
                          }
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        ) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val client = GamesHttpClient(http, "https://axiomnode-gateway.amksandbox.cloud")

        val result = kotlinx.coroutines.runBlocking {
            client.generateGame(
                request = GameGenerateRequest(language = "es", categoryId = "ciencia"),
                authToken = "token",
                gameType = GameType.QUIZ,
            )
        }

        assertTrue(result.isSuccess)
        assertEquals("quiz-model-1", result.getOrThrow().id)
        assertEquals(1, result.getOrThrow().questions.size)
    }

    @Test
    fun `random games requests quiz and wordpass endpoints`() {
        val paths = mutableListOf<String>()
        val http = HttpClient(
            MockEngine { request ->
                paths += request.url.encodedPath
                val payload =
                    if (request.url.encodedPath.endsWith("/quiz/random")) {
                        """
                            {"gameType":"quiz","items":[{"id":"qg-1","categoryId":"ciencia","categoryName":"Ciencia","language":"es","questions":[{"id":"q1","question":"Q","answers":["a","b"],"correctIndex":0}]}]}
                        """.trimIndent()
                    } else {
                        """
                            {"gameType":"wordpass","items":[{"id":"wg-1","categoryId":"historia","categoryName":"Historia","language":"es","words":[{"id":"w1","definition":"Pista","word":"Respuesta"}]}]}
                        """.trimIndent()
                    }

                respond(
                    content = payload,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        ) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val client = GamesHttpClient(http, "https://axiomnode-gateway.amksandbox.cloud")

        val result = kotlinx.coroutines.runBlocking { client.getRandomGames(count = 4, language = "es") }

        assertTrue(result.isSuccess)
        assertTrue(paths.any { it.endsWith("/v1/mobile/games/quiz/random") })
        assertTrue(paths.any { it.endsWith("/v1/mobile/games/wordpass/random") })
    }

    @Test
    fun `random games parse stored models with request response payload`() {
        val http = HttpClient(
            MockEngine { request ->
                val payload =
                    if (request.url.encodedPath.endsWith("/quiz/random")) {
                        """
                            {
                              "gameType":"quiz",
                              "items":[
                                {
                                  "id":"quiz-model-2",
                                  "gameType":"quiz",
                                  "categoryId":"ciencia",
                                  "categoryName":"Ciencia",
                                  "language":"es",
                                  "request":{"categoryId":"ciencia","language":"es"},
                                  "response":{"questions":[{"id":"q1","question":"Q","answers":["a","b"],"correctIndex":0}]}
                                }
                              ]
                            }
                        """.trimIndent()
                    } else {
                        """
                            {
                              "gameType":"word-pass",
                              "items":[
                                {
                                  "id":"word-model-2",
                                  "gameType":"word-pass",
                                  "categoryId":"historia",
                                  "categoryName":"Historia",
                                  "language":"es",
                                  "request":{"categoryId":"historia","language":"es"},
                                  "response":{"words":[{"id":"w1","definition":"Pista","word":"Respuesta"}]}
                                }
                              ]
                            }
                        """.trimIndent()
                    }

                respond(
                    content = payload,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        ) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val client = GamesHttpClient(http, "https://axiomnode-gateway.amksandbox.cloud")
        val result = kotlinx.coroutines.runBlocking { client.getRandomGames(count = 2, language = "es") }

        assertTrue(result.isSuccess)
        val games = result.getOrThrow()
        assertEquals(2, games.size)
        assertTrue(games.any { it.id == "quiz-model-2" && it.questions.isNotEmpty() })
        assertTrue(games.any { it.id == "word-model-2" && it.questions.isNotEmpty() })
    }

    @Test
    fun `sync results uses game events endpoint`() {
        var capturedPath = ""
        val http = HttpClient(
            MockEngine { request ->
                capturedPath = request.url.encodedPath
                respond(
                    content = """{"synced":1,"message":"ok"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        ) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val client = GamesHttpClient(http, "https://axiomnode-gateway.amksandbox.cloud")
        val result = kotlinx.coroutines.runBlocking {
            client.syncGameResults(
                events = listOf(
                    GameEventDto(
                        gameId = "g-1",
                        gameType = "quiz",
                        categoryId = "ciencia",
                        categoryName = "Ciencia",
                        language = "es",
                        outcome = "WON",
                        score = 90,
                        durationSeconds = 120,
                        timestamp = 1710000000000,
                    ),
                ),
                authToken = "firebase-token",
            )
        }

        assertTrue(result.isSuccess)
        assertEquals("/v1/mobile/games/events", capturedPath)
    }
}
