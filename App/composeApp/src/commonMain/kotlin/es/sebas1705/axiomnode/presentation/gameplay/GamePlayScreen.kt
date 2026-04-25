package es.sebas1705.axiomnode.presentation.gameplay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.sebas1705.axiomnode.domain.models.Game
import es.sebas1705.axiomnode.domain.models.GameOutcome
import es.sebas1705.axiomnode.domain.models.GameType
import es.sebas1705.axiomnode.ui.components.ConfirmDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import es.sebas1705.axiomnode.ui.layout.LocalWindowSize
import es.sebas1705.axiomnode.ui.layout.WindowSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamePlayScreen(
    game: Game,
    viewModel: GamePlayViewModel,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmExit by remember { mutableStateOf(false) }
    val windowSize = LocalWindowSize.current
    val horizontalGutter = when (windowSize) {
        WindowSize.COMPACT -> 16.dp
        WindowSize.MEDIUM -> 20.dp
        WindowSize.EXPANDED -> 24.dp
    }

    LaunchedEffect(game.id) {
        viewModel.startGame(game)
    }

    if (confirmExit) {
        ConfirmDialog(
            title = "¿Salir de la partida?",
            message = "Perderás el progreso actual.",
            confirmLabel = "Salir",
            onConfirm = { confirmExit = false; onExit() },
            onDismiss = { confirmExit = false },
        )
    }

    if (state.isFinished) {
        GameResultScreen(
            state = state,
            onPlayAgain = { viewModel.startGame(game) },
            onExit = onExit,
            modifier = modifier,
        )
    } else {
        Scaffold(
            modifier = modifier,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = state.game?.categoryName ?: "",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "Pregunta ${state.currentQuestionIndex + 1} de ${state.totalQuestions}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    actions = {
                        // Timer
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Text(
                                text = formatTime(state.elapsedSeconds),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { confirmExit = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Salir",
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = horizontalGutter),
            ) {
                // ── Progress bar ────────────────────────────────────────
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .padding(vertical = 2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )

                // ── Score badges ────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ScoreBadge(
                        label = "✓ ${state.correctCount}",
                        color = MaterialTheme.colorScheme.primaryContainer,
                        textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    ScoreBadge(
                        label = "✗ ${state.wrongCount}",
                        color = MaterialTheme.colorScheme.errorContainer,
                        textColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }

                // ── Question content ────────────────────────────────────
                state.currentQuestion?.let { question ->
                    AnimatedContent(
                        targetState = state.currentQuestionIndex,
                        transitionSpec = {
                            (slideInHorizontally { it } + fadeIn())
                                .togetherWith(slideOutHorizontally { -it } + fadeOut())
                        },
                        label = "question-transition",
                    ) { targetIndex ->
                        val q = state.game?.questions?.getOrNull(targetIndex) ?: question
                        when (state.game?.gameType) {
                            GameType.WORDPASS -> WordpassContent(
                                question = q,
                                typedAnswer = state.typedAnswer,
                                isRevealed = state.isAnswerRevealed,
                                onTypedAnswerChange = { viewModel.updateTypedAnswer(it) },
                                onSubmit = { viewModel.submitTypedAnswer() },
                            )
                            else -> QuestionContent(
                                question = q,
                                selectedAnswer = state.selectedAnswer,
                                isRevealed = state.isAnswerRevealed,
                                onSelectAnswer = { viewModel.selectAnswer(it) },
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                // ── Next / Skip button ──────────────────────────────────
                AnimatedVisibility(
                    visible = state.isAnswerRevealed,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Button(
                        onClick = { viewModel.nextQuestion() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(bottom = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(
                            text = if (state.isLastQuestion) "Ver Resultados" else "Siguiente",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Question card with answer options
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuestionContent(
    question: es.sebas1705.axiomnode.domain.models.Question,
    selectedAnswer: String?,
    isRevealed: Boolean,
    onSelectAnswer: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        // Question text
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                text = question.text,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(24.dp),
            )
        }

        Spacer(Modifier.height(20.dp))

        // Answer options
        question.options.forEachIndexed { index, option ->
            val isSelected = option == selectedAnswer
            val isCorrect = option == question.correctAnswer

            val containerColor = when {
                !isRevealed && isSelected -> MaterialTheme.colorScheme.primaryContainer
                isRevealed && isCorrect -> MaterialTheme.colorScheme.primaryContainer
                isRevealed && isSelected && !isCorrect -> MaterialTheme.colorScheme.errorContainer
                else -> Color.Transparent
            }
            val borderColor = when {
                !isRevealed && isSelected -> MaterialTheme.colorScheme.primary
                isRevealed && isCorrect -> MaterialTheme.colorScheme.primary
                isRevealed && isSelected && !isCorrect -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.outlineVariant
            }
            val textColor = when {
                !isRevealed && isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                isRevealed && isCorrect -> MaterialTheme.colorScheme.onPrimaryContainer
                isRevealed && isSelected && !isCorrect -> MaterialTheme.colorScheme.onErrorContainer
                else -> MaterialTheme.colorScheme.onSurface
            }

            val prefix = ('A' + index).toString()

            OutlinedCard(
                onClick = { if (!isRevealed) onSelectAnswer(option) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
                border = BorderStroke(
                    width = if (isSelected || (isRevealed && isCorrect)) 2.dp else 1.dp,
                    color = borderColor,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected || (isRevealed && isCorrect))
                            borderColor
                        else
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = prefix,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected || (isRevealed && isCorrect))
                                    MaterialTheme.colorScheme.surface
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor,
                        modifier = Modifier.weight(1f),
                    )
                    if (isRevealed) {
                        Text(
                            text = if (isCorrect) "✓" else if (isSelected) "✗" else "",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isCorrect)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Game result screen
// ─────────────────────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────────────────────────
// Wordpass: text-input answer mode
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WordpassContent(
    question: es.sebas1705.axiomnode.domain.models.Question,
    typedAnswer: String,
    isRevealed: Boolean,
    onTypedAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        // Question text
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                text = question.text,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(24.dp),
            )
        }

        Spacer(Modifier.height(20.dp))

        // Text input field
        OutlinedTextField(
            value = typedAnswer,
            onValueChange = onTypedAnswerChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isRevealed,
            label = { Text("Tu respuesta") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        )

        Spacer(Modifier.height(12.dp))

        // Submit button
        if (!isRevealed) {
            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = typedAnswer.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Comprobar", style = MaterialTheme.typography.labelLarge)
            }
        }

        // Feedback after reveal
        if (isRevealed) {
            Spacer(Modifier.height(12.dp))
            val isCorrect = typedAnswer.trim().equals(question.correctAnswer.trim(), ignoreCase = true)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCorrect)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isCorrect) "✓ ¡Correcto!" else "✗ Incorrecto",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isCorrect)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onErrorContainer,
                    )
                    if (!isCorrect) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Respuesta correcta: ${question.correctAnswer}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Game result screen (original marker)
// ─────────────────────────────────────────────────────────────────────────────

// GameResultScreen has been moved to GameResultScreen.kt

// ─────────────────────────────────────────────────────────────────────────────
// Helper composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ScoreBadge(
    label: String,
    color: Color,
    textColor: Color,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}


private fun formatTime(seconds: Int): String {
    val min = seconds / 60
    val sec = seconds % 60
    return "${min.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}"
}




