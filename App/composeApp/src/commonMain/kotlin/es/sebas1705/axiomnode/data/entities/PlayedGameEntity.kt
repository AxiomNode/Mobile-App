package es.sebas1705.axiomnode.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import es.sebas1705.axiomnode.domain.models.Game
import es.sebas1705.axiomnode.domain.models.GameOutcome
import es.sebas1705.axiomnode.domain.models.GameType
import es.sebas1705.axiomnode.domain.models.Question
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "played_games")
data class PlayedGameEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameId: String,
    val gameType: String,
    val categoryId: String,
    val categoryName: String,
    val language: String,
    val questionsJson: String,
    val playedAt: Long,
    val outcome: String,
    val score: Int,
) {
    fun toDomainGame(): Game {
        val questions = try {
            Json.decodeFromString<List<Question>>(questionsJson)
        } catch (_: Exception) {
            emptyList()
        }

        return Game(
            id = gameId,
            gameType = GameType.valueOf(gameType),
            categoryId = categoryId,
            categoryName = categoryName,
            language = language,
            questions = questions,
        )
    }

    companion object {
        fun from(game: Game, playedAt: Long, outcome: GameOutcome, score: Int): PlayedGameEntity =
            PlayedGameEntity(
                gameId = game.id,
                gameType = game.gameType.name,
                categoryId = game.categoryId,
                categoryName = game.categoryName,
                language = game.language,
                questionsJson = Json.encodeToString(game.questions),
                playedAt = playedAt,
                outcome = outcome.name,
                score = score,
            )
    }
}
