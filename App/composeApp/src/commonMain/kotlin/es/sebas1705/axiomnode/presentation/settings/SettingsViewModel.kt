package es.sebas1705.axiomnode.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.sebas1705.axiomnode.data.preferences.PreferencesRepository
import es.sebas1705.axiomnode.data.preferences.ThemeMode
import es.sebas1705.axiomnode.domain.usecases.GamesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ContentDownloadState(
    val isRunning: Boolean = false,
    val downloaded: Int = 0,
    val attempted: Int = 0,
    val target: Int = 0,
    val maxAttempts: Int = 0,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
) {
    val progress: Float
        get() {
            val downloadProgress = if (target > 0) downloaded.toFloat() / target.toFloat() else 0f
            val attemptProgress = if (maxAttempts > 0) attempted.toFloat() / maxAttempts.toFloat() else 0f
            return maxOf(downloadProgress, attemptProgress).coerceIn(0f, 1f)
        }
}

class SettingsViewModel(
    private val prefs: PreferencesRepository,
    private val gamesUseCase: GamesUseCase,
) : ViewModel() {
    private companion object {
        const val MASSIVE_DOWNLOAD_TARGET = 400
        const val DOWNLOAD_BATCH_SIZE = 10
    }

    val state: StateFlow<es.sebas1705.axiomnode.data.preferences.UserPreferences> = prefs.preferences
    private val _downloadState = MutableStateFlow(ContentDownloadState())
    val downloadState: StateFlow<ContentDownloadState> = _downloadState.asStateFlow()

    fun setThemeMode(mode: ThemeMode) = prefs.updateThemeMode(mode)
    fun setDifficulty(value: Int) = prefs.updateDefaultDifficulty(value)
    fun setNumQuestions(value: Int) = prefs.updateDefaultNumQuestions(value)
    fun setAnalytics(enabled: Boolean) = prefs.updateAnalyticsEnabled(enabled)

    fun downloadMassiveDistributedContent() {
        if (_downloadState.value.isRunning) return

        viewModelScope.launch {
            _downloadState.value = ContentDownloadState(
                isRunning = true,
                downloaded = 0,
                target = MASSIVE_DOWNLOAD_TARGET,
                attempted = 0,
                maxAttempts = 0,
                statusMessage = "Preparing distributed download...",
            )

            val catalog = gamesUseCase.getGameCatalog().getOrElse { error ->
                _downloadState.value = ContentDownloadState(
                    isRunning = false,
                    downloaded = 0,
                    target = MASSIVE_DOWNLOAD_TARGET,
                    attempted = 0,
                    maxAttempts = 0,
                    errorMessage = error.message ?: "Failed to read the catalog for content download",
                )
                return@launch
            }

            val categories = catalog.categories.map { it.id }
            val downloadPlan = buildDownloadPlan(categories)

            var downloaded = 0
            var attempt = 0
            var cursor = 0
            var lastError: String? = null
            val maxAttempts = (downloadPlan.size * 8).coerceAtLeast(24)

            _downloadState.value = _downloadState.value.copy(maxAttempts = maxAttempts)

            while (downloaded < MASSIVE_DOWNLOAD_TARGET && attempt < maxAttempts) {
                val categoryId = downloadPlan[cursor % downloadPlan.size]
                cursor += 1
                attempt += 1

                val remaining = MASSIVE_DOWNLOAD_TARGET - downloaded
                val batchSize = minOf(DOWNLOAD_BATCH_SIZE, remaining)

                _downloadState.value = _downloadState.value.copy(
                    statusMessage = "Downloading distributed content ($downloaded/$MASSIVE_DOWNLOAD_TARGET)",
                    attempted = attempt,
                    errorMessage = null,
                )

                val result = gamesUseCase.getRandomGames(
                    count = batchSize,
                    language = "en",
                    categoryId = categoryId,
                )

                if (result.isSuccess) {
                    downloaded += result.getOrNull().orEmpty().size
                } else {
                    lastError = result.exceptionOrNull()?.message
                }

                _downloadState.value = _downloadState.value.copy(
                    downloaded = downloaded,
                    attempted = attempt,
                )
            }

            if (downloaded > 0) {
                _downloadState.value = ContentDownloadState(
                    isRunning = false,
                    downloaded = downloaded,
                    target = MASSIVE_DOWNLOAD_TARGET,
                    attempted = attempt,
                    maxAttempts = maxAttempts,
                    statusMessage = "Download complete: $downloaded items cached.",
                )
            } else {
                _downloadState.value = ContentDownloadState(
                    isRunning = false,
                    downloaded = 0,
                    target = MASSIVE_DOWNLOAD_TARGET,
                    attempted = attempt,
                    maxAttempts = maxAttempts,
                    errorMessage = lastError ?: "Unable to download content right now",
                )
            }
        }
    }

    private fun buildDownloadPlan(
        categories: List<String>,
    ): List<String?> {
        val plan = mutableListOf<String?>()
        plan += null
        categories.forEach { categoryId ->
            plan += categoryId
        }
        return plan.ifEmpty { listOf(null) }
    }
}

