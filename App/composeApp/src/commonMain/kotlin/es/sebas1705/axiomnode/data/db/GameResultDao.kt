package es.sebas1705.axiomnode.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import es.sebas1705.axiomnode.data.entities.GameResultEntity

@Dao
interface GameResultDao {
    @Query("SELECT * FROM game_results WHERE id = :id LIMIT 1")
    suspend fun getResultById(id: Long): GameResultEntity?

    @Query("SELECT * FROM game_results ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentResults(limit: Int = 20): List<GameResultEntity>

    @Query("SELECT * FROM game_results WHERE synced = 0")
    suspend fun getUnSyncedResults(): List<GameResultEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: GameResultEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResults(results: List<GameResultEntity>)

    @Query("UPDATE game_results SET synced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    @Query("DELETE FROM game_results WHERE timestamp < :olderThanMs")
    suspend fun deleteOldResults(olderThanMs: Long)
}

