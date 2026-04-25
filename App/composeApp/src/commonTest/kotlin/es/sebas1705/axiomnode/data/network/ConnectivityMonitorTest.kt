package es.sebas1705.axiomnode.data.network

import es.sebas1705.axiomnode.config.AppConfig
import es.sebas1705.axiomnode.config.AppEnvironment
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConnectivityMonitorTest {

    @Test
    fun `refreshNow is online when versioned health responds ok`() {
        val paths = mutableListOf<String>()
        val client = HttpClient(
            MockEngine { request ->
                paths += request.url.encodedPath
                if (request.url.encodedPath == "/v1/health") {
                    respond("ok", HttpStatusCode.OK)
                } else {
                    respond("missing", HttpStatusCode.NotFound)
                }
            },
        )

        val monitor = ConnectivityMonitor(
            httpClient = client,
            config = config("https://axiomnode-gateway.amksandbox.cloud"),
            autoRefresh = false,
        )

        val online = runBlocking { monitor.refreshNow() }
        monitor.stop()

        assertTrue(online)
        assertTrue(monitor.isOnline.value)
        assertTrue(paths.contains("/v1/health"))
    }

    @Test
    fun `refreshNow falls back to root health when versioned health fails`() {
        val paths = mutableListOf<String>()
        val client = HttpClient(
            MockEngine { request ->
                paths += request.url.encodedPath
                when (request.url.encodedPath) {
                    "/v1/health" -> respond("missing", HttpStatusCode.NotFound)
                    "/health" -> respond("ok", HttpStatusCode.OK)
                    else -> respond("missing", HttpStatusCode.NotFound)
                }
            },
        )

        val monitor = ConnectivityMonitor(
            httpClient = client,
            config = config("https://axiomnode-gateway.amksandbox.cloud"),
            autoRefresh = false,
        )

        val online = runBlocking { monitor.refreshNow() }
        monitor.stop()

        assertTrue(online)
        assertEquals(listOf("/v1/health", "/health"), paths)
        assertTrue(monitor.isOnline.value)
    }

    @Test
    fun `refreshNow sets offline when all probes fail`() {
        val client = HttpClient(
            MockEngine {
                respond("down", HttpStatusCode.ServiceUnavailable)
            },
        )

        val monitor = ConnectivityMonitor(
            httpClient = client,
            config = config("https://axiomnode-gateway.amksandbox.cloud"),
            autoRefresh = false,
        )

        val online = runBlocking { monitor.refreshNow() }
        monitor.stop()

        assertFalse(online)
        assertFalse(monitor.isOnline.value)
    }

    private fun config(apiBaseUrl: String): AppConfig = AppConfig(
        environment = AppEnvironment.DEV,
        apiBaseUrl = apiBaseUrl,
        authMode = "firebase",
        firebaseApiKey = "",
        firebaseAuthDomain = "",
        firebaseProjectId = "",
        firebaseStorageBucket = "",
        firebaseMessagingSenderId = "",
        firebaseAppId = "",
        firebaseMeasurementId = "",
        googleWebClientId = "",
    )
}
