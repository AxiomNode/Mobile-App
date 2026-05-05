package es.sebas1705.axiomnode.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import es.sebas1705.axiomnode.data.entities.GameEntity
import es.sebas1705.axiomnode.data.entities.QuizGameEntity
import es.sebas1705.axiomnode.data.entities.WordpassGameEntity

@Dao
interface GameDao {
    @Query(
        """
        SELECT * FROM (
            SELECT id, 'QUIZ' AS gameType, categoryId, categoryName, language, questionsJson, createdAt
            FROM quiz_games_cache
            WHERE id = :id
            UNION ALL
            SELECT id, 'WORDPASS' AS gameType, categoryId, categoryName, language, questionsJson, createdAt
            FROM wordpass_games_cache
            WHERE id = :id
        )
        ORDER BY createdAt DESC
        LIMIT 1
        """,
    )
    suspend fun getGameById(id: String): GameEntity?

    @Query(
        """
        SELECT * FROM (
            SELECT id, 'QUIZ' AS gameType, categoryId, categoryName, language, questionsJson, createdAt
            FROM quiz_games_cache
            UNION ALL
            SELECT id, 'WORDPASS' AS gameType, categoryId, categoryName, language, questionsJson, createdAt
            FROM wordpass_games_cache
        )
        ORDER BY createdAt DESC
        LIMIT :limit
        """,
    )
    suspend fun getRecentGames(limit: Int = 10): List<GameEntity>

    @Query(
        """
        SELECT * FROM (
            SELECT id, 'QUIZ' AS gameType, categoryId, categoryName, language, questionsJson, createdAt
            FROM quiz_games_cache
            UNION ALL
            SELECT id, 'WORDPASS' AS gameType, categoryId, categoryName, language, questionsJson, createdAt
            FROM wordpass_games_cache
        )
        WHERE id IN (:ids)
        """,
    )
    suspend fun getGamesByIds(ids: List<String>): List<GameEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizGameRow(game: QuizGameEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWordpassGameRow(game: WordpassGameEntity)

    @Transaction
    suspend fun insertGame(game: GameEntity) {
        when (game.gameType.uppercase()) {
            "QUIZ" -> insertQuizGameRow(QuizGameEntity.fromGameEntity(game))
            "WORDPASS" -> insertWordpassGameRow(WordpassGameEntity.fromGameEntity(game))
            else -> throw IllegalArgumentException("gameType no soportado para cache: ${game.gameType}")
        }
    }

    suspend fun insertGames(games: List<GameEntity>) {
        games.forEach { game ->
            insertGame(game)
        }
    }

    @Query("DELETE FROM quiz_games_cache")
    suspend fun clearQuizGames()

    @Query("DELETE FROM wordpass_games_cache")
    suspend fun clearWordpassGames()

    @Transaction
    suspend fun clearAllGames() {
        clearQuizGames()
        clearWordpassGames()
    }

    @Query("DELETE FROM quiz_games_cache WHERE createdAt < :olderThanMs")
    suspend fun deleteOldQuizGames(olderThanMs: Long)

    @Query("DELETE FROM wordpass_games_cache WHERE createdAt < :olderThanMs")
    suspend fun deleteOldWordpassGames(olderThanMs: Long)

    @Transaction
    suspend fun deleteOldGames(olderThanMs: Long) {
        deleteOldQuizGames(olderThanMs)
        deleteOldWordpassGames(olderThanMs)
    }
}

