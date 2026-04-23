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
                              {"id":"q1","question":"2+2","options":["4","5"],"correct_index":0}
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
                              {"id":"w1","hint":"Capital de Francia","answer":"Paris"}
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
    fun `random games requests quiz and wordpass endpoints`() {
        val paths = mutableListOf<String>()
        val http = HttpClient(
            MockEngine { request ->
                paths += request.url.encodedPath
                val payload =
                    if (request.url.encodedPath.endsWith("/quiz/random")) {
                        """
                            {"gameType":"quiz","items":[{"id":"qg-1","categoryId":"ciencia","categoryName":"Ciencia","language":"es","questions":[{"id":"q1","question":"Q","options":["a","b"],"correct_index":0}]}]}
                        """.trimIndent()
                    } else {
                        """
                            {"gameType":"wordpass","items":[{"id":"wg-1","categoryId":"historia","categoryName":"Historia","language":"es","words":[{"id":"w1","hint":"Pista","answer":"Respuesta"}]}]}
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
}
