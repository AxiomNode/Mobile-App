package es.sebas1705.axiomnode.di

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import es.sebas1705.axiomnode.data.db.AxiomNodeDatabase
import java.io.File

actual fun provideDatabase(): AxiomNodeDatabase {
    val dbPath = run {
        val dbDir = File(System.getProperty("user.home"), ".axiomnode")
        dbDir.mkdirs()
        File(dbDir, "axiomnode.db").absolutePath
    }

    return Room.databaseBuilder<AxiomNodeDatabase>(
        name = dbPath,
    )
        .setDriver(BundledSQLiteDriver())
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
}

