package es.sebas1705.axiomnode.data.network

import es.sebas1705.axiomnode.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Lightweight connectivity monitor based on API gateway reachability.
 */
class ConnectivityMonitor(
    private val httpClient: HttpClient,
    private val config: AppConfig,
    private val intervalMs: Long = 15_000L,
    private val probeTimeoutMs: Long = 3_500L,
    autoRefresh: Boolean = true,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _isOnline = MutableStateFlow(true)

    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        if (autoRefresh) {
            scope.launch {
                while (isActive) {
                    refreshNow()
                    delay(intervalMs)
                }
            }
        }
    }

    suspend fun refreshNow(): Boolean {
        val online = probeGateway()
        _isOnline.value = online
        return online
    }

    fun stop() {
        scope.cancel()
    }

    private suspend fun probeGateway(): Boolean {
        val baseUrl = config.apiBaseUrl.trimEnd('/')
        val probes = listOf(
            "$baseUrl/v1/health",
            "$baseUrl/health",
        ).distinct()

        probes.forEach { url ->
            val reachable = runCatching {
                withTimeout(probeTimeoutMs) {
                    val response = httpClient.get(url)
                    response.status.value in 200..399
                }
            }.getOrDefault(false)

            if (reachable) {
                return true
            }
        }

        return false
    }
}