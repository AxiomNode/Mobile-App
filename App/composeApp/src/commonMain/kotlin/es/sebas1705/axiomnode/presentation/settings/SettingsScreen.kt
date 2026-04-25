package es.sebas1705.axiomnode.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val windowSize = LocalWindowSize.current
    val horizontalGutter = when (windowSize) {
        WindowSize.COMPACT -> 16.dp
        WindowSize.MEDIUM -> 20.dp
        WindowSize.EXPANDED -> 24.dp
    }

    AppScaffold(
        title = "Settings",
        modifier = modifier,
        onBack = { navigator.pop() },
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader(title = "Appearance")
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

            SectionHeader(title = "Default gameplay")
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalGutter),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Language: English only", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(16.dp))

                    Text("Default difficulty: ${prefs.defaultDifficulty}%",
                        style = MaterialTheme.typography.titleSmall)
                    Slider(
                        value = prefs.defaultDifficulty.toFloat(),
                        onValueChange = { viewModel.setDifficulty(it.toInt()) },
                        valueRange = 0f..100f,
                        steps = 9,
                    )

                    Spacer(Modifier.height(8.dp))
                    Text("Default question count: ${prefs.defaultNumQuestions}",
                        style = MaterialTheme.typography.titleSmall)
                    Slider(
                        value = prefs.defaultNumQuestions.toFloat(),
                        onValueChange = { viewModel.setNumQuestions(it.toInt()) },
                        valueRange = 5f..30f,
                        steps = 24,
                    )
                }
            }

            SectionHeader(title = "Privacy")
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalGutter),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                shape = RoundedCornerShape(14.dp),
            ) {
                ListItem(
                    headlineContent = { Text("Share anonymous analytics") },
                    supportingContent = { Text("Helps improve the experience.") },
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

            SectionHeader(title = "Offline content")
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalGutter),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Download a large cache distributed across categories.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Target: around 400 items (200 quiz + 200 wordpass).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Button(
                        onClick = { viewModel.downloadMassiveDistributedContent() },
                        enabled = !downloadState.isRunning,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (downloadState.isRunning) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                                Text("Downloading...")
                            }
                        } else {
                            Text("Download distributed content")
                        }
                    }

                    downloadState.statusMessage?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    if (downloadState.isRunning || downloadState.downloaded > 0 || downloadState.attempted > 0) {
                        val attemptsLabel = if (downloadState.maxAttempts > 0) {
                            "${downloadState.attempted}/${downloadState.maxAttempts} attempts"
                        } else {
                            "${downloadState.attempted} attempts"
                        }

                        Text(
                            text = "Progress: ${downloadState.downloaded}/${downloadState.target} saved · $attemptsLabel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        LinearProgressIndicator(
                            progress = { downloadState.progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    downloadState.errorMessage?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "Follow system"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

