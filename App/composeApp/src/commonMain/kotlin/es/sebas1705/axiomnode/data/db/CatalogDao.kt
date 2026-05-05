package es.sebas1705.axiomnode.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import es.sebas1705.axiomnode.data.entities.CatalogCategoryEntity
import es.sebas1705.axiomnode.data.entities.CatalogLanguageEntity
import es.sebas1705.axiomnode.data.entities.CatalogSyncStateEntity

@Dao
interface CatalogDao {
    @Query("SELECT * FROM catalog_categories ORDER BY name ASC")
    suspend fun getCategories(): List<CatalogCategoryEntity>

    @Query("SELECT * FROM catalog_languages ORDER BY name ASC")
    suspend fun getLanguages(): List<CatalogLanguageEntity>

    @Query("SELECT * FROM catalog_sync_state WHERE id = 1 LIMIT 1")
    suspend fun getSyncState(): CatalogSyncStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(items: List<CatalogCategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLanguages(items: List<CatalogLanguageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncState(state: CatalogSyncStateEntity)

    @Query("DELETE FROM catalog_categories")
    suspend fun clearCategories()

    @Query("DELETE FROM catalog_languages")
    suspend fun clearLanguages()

    @Transaction
    suspend fun replaceCatalog(
        categories: List<CatalogCategoryEntity>,
        languages: List<CatalogLanguageEntity>,
        lastSyncAt: Long,
        catalogHash: String,
    ) {
        clearCategories()
        clearLanguages()
        if (categories.isNotEmpty()) {
            insertCategories(categories)
        }
        if (languages.isNotEmpty()) {
            insertLanguages(languages)
        }
        upsertSyncState(
            CatalogSyncStateEntity(
                id = 1,
                lastSyncAt = lastSyncAt,
                catalogHash = catalogHash,
            ),
        )
    }
}
