package es.sebas1705.axiomnode.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import es.sebas1705.axiomnode.domain.models.Game
import es.sebas1705.axiomnode.domain.models.GameOutcome
import es.sebas1705.axiomnode.domain.models.GameType

private data class OutcomeVisual(
    val label: String,
    val icon: ImageVector,
    val container: Color,
    val content: Color,
)

// ─────────────────────────────────────────────────────────────────────────────
// Badges & chips
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GameTypeBadge(gameType: GameType, modifier: Modifier = Modifier) {
    val (container, content) = when (gameType) {
        GameType.QUIZ -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        GameType.WORDPASS -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }
    val label = when (gameType) {
        GameType.QUIZ -> "QUIZ"
        GameType.WORDPASS -> "WORDPASS"
    }
    Surface(modifier = modifier, color = container, shape = RoundedCornerShape(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun OutcomeBadge(outcome: GameOutcome, modifier: Modifier = Modifier) {
    val visual = when (outcome) {
        GameOutcome.WON -> OutcomeVisual(
            label = "Victoria",
            icon = Icons.Outlined.EmojiEvents,
            container = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        GameOutcome.DRAW -> OutcomeVisual(
            label = "Empate",
            icon = Icons.Outlined.TipsAndUpdates,
            container = MaterialTheme.colorScheme.tertiaryContainer,
            content = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        GameOutcome.LOST -> OutcomeVisual(
            label = "Derrota",
            icon = Icons.Outlined.Cancel,
            container = MaterialTheme.colorScheme.errorContainer,
            content = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
    Surface(modifier = modifier, color = visual.container, shape = RoundedCornerShape(10.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = visual.icon,
                contentDescription = null,
                tint = visual.content,
            )
            Text(
                text = visual.label,
                style = MaterialTheme.typography.labelSmall,
                color = visual.content,
            )
        }
    }
}

@Composable
fun DetailChip(
    label: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    content: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Surface(modifier = modifier, color = container, shape = RoundedCornerShape(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GameCard – shared between Home, Catalog, History
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GameCard(
    game: Game,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    actionLabel: String = "Jugar",
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = game.categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                GameTypeBadge(game.gameType)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailChip("${game.questions.size} preguntas")
                DetailChip(game.language.uppercase())
            }
            if (onClick != null) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(actionLabel, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Stats tile + win-rate bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun WinRateBar(
    winRate: Float,
    label: String = "Tasa de victoria",
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${(winRate * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { winRate.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sync banner – shown when there are pending unsynced game results
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SyncBanner(
    pendingCount: Int,
    isSyncing: Boolean,
    onSync: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Sync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Text(
                        text = "$pendingCount resultado${if (pendingCount != 1) "s" else ""} pendiente${if (pendingCount != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
                if (!message.isNullOrBlank()) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                    )
                }
            }
            OutlinedButton(
                onClick = onSync,
                enabled = !isSyncing,
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(if (isSyncing) "Sincronizando…" else "Sincronizar")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Confirm dialog (logout, exit game…)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "Aceptar",
    dismissLabel: String = "Cancelar",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(dismissLabel) }
        },
    )
}

