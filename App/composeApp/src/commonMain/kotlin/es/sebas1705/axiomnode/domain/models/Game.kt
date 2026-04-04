package es.sebas1705.axiomnode.domain.models

/**
 * Modelos para el juego de Quiz.
 */
data class Game(
    val id: String,
    val gameType: GameType,
    val categoryId: String,
    val categoryName: String,
    val language: String,
    val questions: List<Question>,
)

data class Question(
    val id: String,
    val text: String,
    val options: List<String>,
    val correctAnswer: String,
)

data class GameResult(
    val gameId: String,
    val gameType: GameType,
    val categoryId: String,
    val categoryName: String,
    val language: String,
    val outcome: GameOutcome,
    val score: Int,
    val durationSeconds: Int,
    val timestamp: Long = System.currentTimeMillis(),
)

enum class GameType {
    QUIZ, WORDPASS
}

enum class GameOutcome {
    WON, LOST, DRAW
}

