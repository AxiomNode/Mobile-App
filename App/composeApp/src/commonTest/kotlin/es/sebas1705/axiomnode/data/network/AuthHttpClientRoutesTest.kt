package es.sebas1705.axiomnode.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthHttpClientRoutesTest {

    @Test
    fun `sync session uses backoffice auth session endpoint`() {
        var capturedPath = ""
        val http = HttpClient(
            MockEngine { request ->
                capturedPath = request.url.encodedPath
                respond(
                    content = """{"message":"ok","firebaseUid":"uid-1","role":"gamer"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        ) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val client = AuthHttpClient(http, "https://axiomnode-gateway.amksandbox.cloud")
        val result = runBlocking { client.syncSessionFromFirebase("firebase-token") }

        assertTrue(result.isSuccess)
        assertEquals("/v1/backoffice/auth/session", capturedPath)
    }

    @Test
    fun `get profile uses backoffice auth me endpoint`() {
        var capturedPath = ""
        val http = HttpClient(
            MockEngine { request ->
                capturedPath = request.url.encodedPath
                respond(
                    content = """
                        {
                          "role": "gamer",
                          "profile": {
                            "firebaseUid": "uid-1",
                            "email": "test@axiomnode.es",
                            "displayName": "Test"
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

        val client = AuthHttpClient(http, "https://axiomnode-gateway.amksandbox.cloud")
        val result = runBlocking { client.getUserProfile("firebase-token") }

        assertTrue(result.isSuccess)
        assertEquals("/v1/backoffice/auth/me", capturedPath)
    }
}
