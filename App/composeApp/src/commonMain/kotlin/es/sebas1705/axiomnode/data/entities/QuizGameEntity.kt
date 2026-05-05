package es.sebas1705.axiomnode.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_games_cache")
data class QuizGameEntity(
    @PrimaryKey
    val id: String,
    val categoryId: String,
    val categoryName: String,
    val language: String,
    val questionsJson: String,
    val createdAt: Long,
) {
    fun toGameEntity(): GameEntity = GameEntity(
        id = id,
        gameType = "QUIZ",
        categoryId = categoryId,
        categoryName = categoryName,
        language = language,
        questionsJson = questionsJson,
        createdAt = createdAt,
    )

    companion object {
        fun fromGameEntity(game: GameEntity): QuizGameEntity {
            require(game.gameType.uppercase() == "QUIZ") { "QuizGameEntity solo acepta gameType QUIZ" }
            return QuizGameEntity(
                id = game.id,
                categoryId = game.categoryId,
                categoryName = game.categoryName,
                language = game.language,
                questionsJson = game.questionsJson,
                createdAt = game.createdAt,
            )
        }
    }
}
