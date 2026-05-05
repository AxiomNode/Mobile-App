package es.sebas1705.axiomnode.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import es.sebas1705.axiomnode.data.entities.CatalogCategoryEntity
import es.sebas1705.axiomnode.data.entities.CatalogLanguageEntity
import es.sebas1705.axiomnode.data.entities.CatalogSyncStateEntity
import es.sebas1705.axiomnode.data.entities.PlayedGameEntity
import es.sebas1705.axiomnode.data.entities.GameResultEntity
import es.sebas1705.axiomnode.data.entities.QuizGameEntity
import es.sebas1705.axiomnode.data.entities.UserProfileEntity
import es.sebas1705.axiomnode.data.entities.WordpassGameEntity

@Database(
    entities = [
        QuizGameEntity::class,
        WordpassGameEntity::class,
        GameResultEntity::class,
        PlayedGameEntity::class,
        UserProfileEntity::class,
        CatalogCategoryEntity::class,
        CatalogLanguageEntity::class,
        CatalogSyncStateEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
@ConstructedBy(AxiomNodeDatabaseConstructor::class)
abstract class AxiomNodeDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun gameResultDao(): GameResultDao
    abstract fun playedGameDao(): PlayedGameDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun catalogDao(): CatalogDao
}

@Suppress("KotlinNoActualForExpect")
expect object AxiomNodeDatabaseConstructor : RoomDatabaseConstructor<AxiomNodeDatabase> {
    override fun initialize(): AxiomNodeDatabase
}

