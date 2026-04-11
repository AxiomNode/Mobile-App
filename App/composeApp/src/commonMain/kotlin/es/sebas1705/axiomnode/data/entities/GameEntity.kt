package es.sebas1705.axiomnode.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import es.sebas1705.axiomnode.domain.models.Game
import es.sebas1705.axiomnode.domain.models.Question
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Entidad Room para almacenar juegos en cache local.
 */
@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey
    val id: String,
    val gameType: String, // QUIZ, WORDPASS
    val categoryId: String,
    val categoryName: String,
    val language: String,
    val questionsJson: String, // JSON serializado
    val createdAt: Long = 0L,
) {
    fun toDomain(): Game {
        val questions = try {
            Json.decodeFromString<List<Question>>(questionsJson)
        } catch (e: Exception) {
            emptyList()
        }
        return Game(
            id = id,
            gameType = es.sebas1705.axiomnode.domain.models.GameType.valueOf(gameType),
            categoryId = categoryId,
            categoryName = categoryName,
            language = language,
            questions = questions,
        )
    }

    companion object {
        fun fromDomain(game: Game): GameEntity {
            return GameEntity(
                id = game.id,
                gameType = game.gameType.name,
                categoryId = game.categoryId,
                categoryName = game.categoryName,
                language = game.language,
                questionsJson = Json.encodeToString(game.questions),
            )
        }
    }
}

