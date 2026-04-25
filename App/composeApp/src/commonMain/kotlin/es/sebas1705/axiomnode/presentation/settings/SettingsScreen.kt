package es.sebas1705.axiomnode.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.sebas1705.axiomnode.data.preferences.ThemeMode
import es.sebas1705.axiomnode.presentation.navigation.Navigator
import es.sebas1705.axiomnode.ui.components.AppScaffold
import es.sebas1705.axiomnode.ui.components.SectionHeader
import es.sebas1705.axiomnode.ui.layout.LocalWindowSize
import es.sebas1705.axiomnode.ui.layout.WindowSize

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val prefs by viewModel.state.collectAsStateWithLifecycle()
    val windowSize = LocalWindowSize.current
    val horizontalGutter = when (windowSize) {
        WindowSize.COMPACT -> 16.dp
        WindowSize.MEDIUM -> 20.dp
        WindowSize.EXPANDED -> 24.dp
    }

    AppScaffold(
        title = "Ajustes",
        modifier = modifier,
        onBack = { navigator.pop() },
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader(title = "Apariencia")
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalGutter),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        ListItem(
                            headlineContent = { Text(themeLabel(mode)) },
                            trailingContent = {
                                RadioButton(
                                    selected = prefs.themeMode == mode,
                                    onClick = { viewModel.setThemeMode(mode) },
                                )
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            SectionHeader(title = "Partidas por defecto")
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalGutter),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Idioma por defecto", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    val langs = listOf("es" to "Español", "en" to "English")
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        langs.forEachIndexed { i, (code, name) ->
                            SegmentedButton(
                                selected = prefs.defaultLanguage == code,
                                onClick = { viewModel.setLanguage(code) },
                                shape = SegmentedButtonDefaults.itemShape(i, langs.size),
                            ) { Text(name) }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(16.dp))

                    Text("Dificultad por defecto: ${prefs.defaultDifficulty}%",
                        style = MaterialTheme.typography.titleSmall)
                    Slider(
                        value = prefs.defaultDifficulty.toFloat(),
                        onValueChange = { viewModel.setDifficulty(it.toInt()) },
                        valueRange = 0f..100f,
                        steps = 9,
                    )

                    Spacer(Modifier.height(8.dp))
                    Text("Preguntas por defecto: ${prefs.defaultNumQuestions}",
                        style = MaterialTheme.typography.titleSmall)
                    Slider(
                        value = prefs.defaultNumQuestions.toFloat(),
                        onValueChange = { viewModel.setNumQuestions(it.toInt()) },
                        valueRange = 5f..30f,
                        steps = 24,
                    )
                }
            }

            SectionHeader(title = "Privacidad")
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalGutter),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                shape = RoundedCornerShape(14.dp),
            ) {
                ListItem(
                    headlineContent = { Text("Compartir analíticas anónimas") },
                    supportingContent = { Text("Ayuda a mejorar la experiencia.") },
                    trailingContent = {
                        Switch(
                            checked = prefs.analyticsEnabled,
                            onCheckedChange = { viewModel.setAnalytics(it) },
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "Seguir sistema"
    ThemeMode.LIGHT -> "Claro"
    ThemeMode.DARK -> "Oscuro"
}

