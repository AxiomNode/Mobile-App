package es.sebas1705.axiomnode.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import es.sebas1705.axiomnode.data.entities.GameEntity

@Dao
interface GameDao {
    @Query("SELECT * FROM games WHERE id = :id LIMIT 1")
    suspend fun getGameById(id: String): GameEntity?

    @Query("SELECT * FROM games ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentGames(limit: Int = 10): List<GameEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGames(games: List<GameEntity>)

    @Query("DELETE FROM games")
    suspend fun clearAllGames()

    @Query("DELETE FROM games WHERE createdAt < :olderThanMs")
    suspend fun deleteOldGames(olderThanMs: Long)
}

