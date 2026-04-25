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
    val target: Int = 0,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

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
    fun setLanguage(lang: String) = prefs.updateDefaultLanguage(lang)
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
                statusMessage = "Preparando descarga repartida...",
            )

            val catalog = gamesUseCase.getGameCatalog().getOrElse { error ->
                _downloadState.value = ContentDownloadState(
                    isRunning = false,
                    target = MASSIVE_DOWNLOAD_TARGET,
                    errorMessage = error.message ?: "No se pudo leer el catálogo para descargar contenido",
                )
                return@launch
            }

            val languages = catalog.languages.map { it.code }.ifEmpty { listOf("es") }
            val categories = catalog.categories.map { it.id }
            val downloadPlan = buildDownloadPlan(languages, categories)

            var downloaded = 0
            var attempt = 0
            var cursor = 0
            var lastError: String? = null
            val maxAttempts = (downloadPlan.size * 8).coerceAtLeast(24)

            while (downloaded < MASSIVE_DOWNLOAD_TARGET && attempt < maxAttempts) {
                val (language, categoryId) = downloadPlan[cursor % downloadPlan.size]
                cursor += 1
                attempt += 1

                val remaining = MASSIVE_DOWNLOAD_TARGET - downloaded
                val batchSize = minOf(DOWNLOAD_BATCH_SIZE, remaining)

                _downloadState.value = _downloadState.value.copy(
                    statusMessage = "Descargando contenido repartido ($downloaded/$MASSIVE_DOWNLOAD_TARGET)",
                    errorMessage = null,
                )

                val result = gamesUseCase.getRandomGames(
                    count = batchSize,
                    language = language,
                    categoryId = categoryId,
                )

                if (result.isSuccess) {
                    downloaded += result.getOrNull().orEmpty().size
                } else {
                    lastError = result.exceptionOrNull()?.message
                }

                _downloadState.value = _downloadState.value.copy(downloaded = downloaded)
            }

            if (downloaded > 0) {
                _downloadState.value = ContentDownloadState(
                    isRunning = false,
                    downloaded = downloaded,
                    target = MASSIVE_DOWNLOAD_TARGET,
                    statusMessage = "Descarga completada: $downloaded objetos guardados en caché.",
                )
            } else {
                _downloadState.value = ContentDownloadState(
                    isRunning = false,
                    downloaded = 0,
                    target = MASSIVE_DOWNLOAD_TARGET,
                    errorMessage = lastError ?: "No se pudo descargar contenido en este momento",
                )
            }
        }
    }

    private fun buildDownloadPlan(
        languages: List<String>,
        categories: List<String>,
    ): List<Pair<String, String?>> {
        val plan = mutableListOf<Pair<String, String?>>()
        languages.forEach { language ->
            // Pass category null first so the backend can return mixed content.
            plan += language to null
            categories.forEach { categoryId ->
                plan += language to categoryId
            }
        }
        return plan.ifEmpty { listOf("es" to null) }
    }
}

