package es.sebas1705.axiomnode.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import es.sebas1705.axiomnode.data.entities.PlayedGameEntity

@Dao
interface PlayedGameDao {
    @Query("SELECT * FROM played_games ORDER BY playedAt DESC LIMIT :limit")
    suspend fun getRecentPlayedGames(limit: Int = 20): List<PlayedGameEntity>

    @Query("SELECT * FROM played_games WHERE gameId = :gameId ORDER BY playedAt DESC LIMIT 1")
    suspend fun getLatestPlayedGameByGameId(gameId: String): PlayedGameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayedGame(game: PlayedGameEntity)

    @Query("DELETE FROM played_games WHERE playedAt < :olderThanMs")
    suspend fun deleteOldPlayedGames(olderThanMs: Long)
}
