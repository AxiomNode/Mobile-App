package es.sebas1705.axiomnode.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import es.sebas1705.axiomnode.domain.models.GameResult
import es.sebas1705.axiomnode.domain.models.GameType

/**
 * Entidad Room para historial de juegos jugados.
 */
@Entity(tableName = "game_results")
data class GameResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameId: String,
    val gameType: String, // QUIZ, WORDPASS
    val categoryId: String,
    val categoryName: String,
    val language: String,
    val outcome: String, // WON, LOST, DRAW
    val score: Int,
    val durationSeconds: Int,
    val timestamp: Long = 0L,
    val synced: Boolean = false, // true si se envio al backend
) {
    fun toDomain(): GameResult {
        return GameResult(
            gameId = gameId,
            gameType = GameType.valueOf(gameType),
            categoryId = categoryId,
            categoryName = categoryName,
            language = language,
            outcome = es.sebas1705.axiomnode.domain.models.GameOutcome.valueOf(outcome),
            score = score,
            durationSeconds = durationSeconds,
            timestamp = timestamp,
        )
    }

    companion object {
        fun fromDomain(result: GameResult): GameResultEntity {
            return GameResultEntity(
                gameId = result.gameId,
                gameType = result.gameType.name,
                categoryId = result.categoryId,
                categoryName = result.categoryName,
                language = result.language,
                outcome = result.outcome.name,
                score = result.score,
                durationSeconds = result.durationSeconds,
                timestamp = result.timestamp,
            )
        }
    }
}

