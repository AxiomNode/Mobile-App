package es.sebas1705.axiomnode.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import es.sebas1705.axiomnode.data.entities.UserProfileEntity

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles ORDER BY lastUpdatedAt DESC LIMIT 1")
    suspend fun getLastProfile(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserProfileEntity)

    @Query("DELETE FROM user_profiles")
    suspend fun clearAll()
}
