package es.sebas1705.axiomnode.data.preferences

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Local user preferences. Currently in-memory; persistence can be added later
 * with multiplatform-settings or DataStore (per platform).
 */
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultLanguage: String = "en",
    val defaultDifficulty: Int = 50,
    val defaultNumQuestions: Int = 10,
    val analyticsEnabled: Boolean = true,
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class PreferencesRepository {
    private val _preferences = MutableStateFlow(UserPreferences())
    val preferences: StateFlow<UserPreferences> = _preferences.asStateFlow()

    fun updateThemeMode(mode: ThemeMode) {
        _preferences.value = _preferences.value.copy(themeMode = mode)
    }

    fun updateDefaultLanguage(language: String) {
        _preferences.value = _preferences.value.copy(defaultLanguage = "en")
    }

    fun updateDefaultDifficulty(difficulty: Int) {
        _preferences.value = _preferences.value.copy(
            defaultDifficulty = difficulty.coerceIn(0, 100),
        )
    }

    fun updateDefaultNumQuestions(num: Int) {
        _preferences.value = _preferences.value.copy(
            defaultNumQuestions = num.coerceIn(5, 30),
        )
    }

    fun updateAnalyticsEnabled(enabled: Boolean) {
        _preferences.value = _preferences.value.copy(analyticsEnabled = enabled)
    }
}

