package es.sebas1705.axiomnode.presentation.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.sebas1705.axiomnode.config.AppConfig
import es.sebas1705.axiomnode.presentation.navigation.Navigator
import es.sebas1705.axiomnode.presentation.profile.getPlatformName
import es.sebas1705.axiomnode.ui.components.AppScaffold
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.Image
import es.sebas1705.axiomnode.resources.Res
import es.sebas1705.axiomnode.resources.logo
import es.sebas1705.axiomnode.ui.layout.LocalWindowSize
import es.sebas1705.axiomnode.ui.layout.WindowSize
import org.koin.compose.koinInject

@Composable
fun AboutScreen(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val config: AppConfig = koinInject()
    val windowSize = LocalWindowSize.current
    val horizontalGutter = when (windowSize) {
        WindowSize.COMPACT -> 16.dp
        WindowSize.MEDIUM -> 20.dp
        WindowSize.EXPANDED -> 24.dp
    }
    AppScaffold(
        title = "Acerca de",
        modifier = modifier,
        onBack = { navigator.pop() },
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalGutter, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Image(
                painter = painterResource(Res.drawable.logo),
                contentDescription = "AxiomNode",
                modifier = Modifier.size(120.dp),
            )
            Text(
                text = "AxiomNode",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Aprende jugando.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    InfoRow("Versión", "1.0.0")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    InfoRow("Plataforma", getPlatformName())
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    InfoRow("Entorno", config.environment.name)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    InfoRow("API", config.apiBaseUrl)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "© 2026 AxiomNode",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    ListItem(
        headlineContent = { Text(label, style = MaterialTheme.typography.labelMedium) },
        supportingContent = { Text(value, style = MaterialTheme.typography.bodyMedium) },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    )
}

