package es.sebas1705.axiomnode.domain.models

import kotlinx.serialization.Serializable

/**
 * Modelos para el juego de Quiz.
 */
@Serializable
data class Game(
    val id: String,
    val gameType: GameType,
    val categoryId: String,
    val categoryName: String,
    val language: String,
    val questions: List<Question>,
)

@Serializable
data class Question(
    val id: String,
    val text: String,
    val options: List<String>,
    val correctAnswer: String,
)

@Serializable
data class GameResult(
    val gameId: String,
    val gameType: GameType,
    val categoryId: String,
    val categoryName: String,
    val language: String,
    val outcome: GameOutcome,
    val score: Int,
    val durationSeconds: Int,
    val timestamp: Long = 0L,
)

@Serializable
enum class GameType {
    QUIZ, WORDPASS
}

@Serializable
enum class GameOutcome {
    WON, LOST, DRAW
}

