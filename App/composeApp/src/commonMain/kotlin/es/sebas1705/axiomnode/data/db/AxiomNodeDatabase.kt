package es.sebas1705.axiomnode.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import es.sebas1705.axiomnode.data.entities.GameEntity
import es.sebas1705.axiomnode.data.entities.GameResultEntity

@Database(
    entities = [GameEntity::class, GameResultEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AxiomNodeDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun gameResultDao(): GameResultDao
}

