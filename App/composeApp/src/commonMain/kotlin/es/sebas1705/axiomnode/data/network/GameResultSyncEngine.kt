package es.sebas1705.axiomnode.data.network

import es.sebas1705.axiomnode.core.logD
import es.sebas1705.axiomnode.core.logE
import es.sebas1705.axiomnode.core.logI
import es.sebas1705.axiomnode.data.db.GameResultDao
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State of the sync engine.
 */
data class SyncState(
    val isSyncing: Boolean = false,
    val pendingCount: Int = 0,
    val lastSyncedCount: Int = 0,
    val lastError: String? = null,
)

/**
 * Background sync engine that pushes unsynced game results to the backend.
 *
 * Uses exponential backoff on failure (base 2s, max 60s, up to [maxRetries] attempts).
 * Results are posted in batches of [batchSize].
 */
class GameResultSyncEngine(
    private val gameResultDao: GameResultDao,
    private val gamesHttpClient: GamesHttpClient,
    private val maxRetries: Int = 3,
    private val batchSize: Int = 20,
) {
    private val _state = MutableStateFlow(SyncState())
    val state: StateFlow<SyncState> = _state.asStateFlow()

    /**
     * Sync all unsynced game results to the backend.
     *
     * @param authToken Bearer token for authenticated requests.
     * @return number of results successfully synced.
     */
    suspend fun syncPendingResults(authToken: String): Result<Int> {
        if (_state.value.isSyncing) {
            return Result.success(0) // Already running
        }

        _state.value = _state.value.copy(isSyncing = true, lastError = null)

        return try {
            val unsynced = gameResultDao.getUnSyncedResults()
            _state.value = _state.value.copy(pendingCount = unsynced.size)

            if (unsynced.isEmpty()) {
                this.logI("SyncEngine: no pending results")
                _state.value = _state.value.copy(isSyncing = false, pendingCount = 0)
                return Result.success(0)
            }

            this.logI("SyncEngine: found ${unsynced.size} unsynced results")

            var totalSynced = 0

            // Process in batches
            unsynced.chunked(batchSize).forEach { batch ->
                val events = batch.map { entity ->
                    GameEventDto(
                        gameId = entity.gameId,
                        gameType = entity.gameType,
                        categoryId = entity.categoryId,
                        categoryName = entity.categoryName,
                        language = entity.language,
                        outcome = entity.outcome,
                        score = entity.score,
                        durationSeconds = entity.durationSeconds,
                        timestamp = entity.timestamp,
                    )
                }

                val result = retryWithBackoff(maxRetries) {
                    gamesHttpClient.syncGameResults(events, authToken)
                }

                result
                    .onSuccess {
                        val ids = batch.map { it.id }
                        gameResultDao.markAsSynced(ids)
                        totalSynced += batch.size
                        _state.value = _state.value.copy(
                            pendingCount = _state.value.pendingCount - batch.size,
                        )
                        this.logI("SyncEngine: synced batch of ${batch.size}")
                    }
                    .onFailure { e ->
                        this.logE("SyncEngine: batch failed after retries: ${e.message}")
                        _state.value = _state.value.copy(lastError = e.message)
                        // Don't throw - continue with next batch (partial sync is fine)
                    }
            }

            _state.value = _state.value.copy(
                isSyncing = false,
                lastSyncedCount = totalSynced,
            )
            Result.success(totalSynced)
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isSyncing = false,
                lastError = e.message,
            )
            Result.failure(e)
        }
    }

    /**
     * Returns the current count of unsynced results.
     */
    suspend fun getPendingCount(): Int {
        return gameResultDao.getUnSyncedResults().size
    }

    /**
     * Retry [block] with exponential backoff.
     * Base delay = 2s, capped at 60s.
     */
    private suspend fun <T> retryWithBackoff(
        maxAttempts: Int,
        block: suspend () -> Result<T>,
    ): Result<T> {
        var lastError: Throwable? = null

        repeat(maxAttempts) { attempt ->
            val result = block()
            if (result.isSuccess) return result

            lastError = result.exceptionOrNull()
            if (attempt < maxAttempts - 1) {
                val delayMs = minOf(2000L * (1L shl attempt), 60_000L)
                this.logD("SyncEngine: attempt ${attempt + 1} failed, retrying in ${delayMs}ms")
                delay(delayMs)
            }
        }

        return Result.failure(lastError ?: Exception("Sync failed after $maxAttempts attempts"))
    }
}

