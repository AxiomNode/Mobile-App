package es.sebas1705.axiomnode.di

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import es.sebas1705.axiomnode.data.db.AxiomNodeDatabase
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSTemporaryDirectory

actual fun provideDatabase(): AxiomNodeDatabase {
    val dbPath = run {
        val docs = NSSearchPathForDirectoriesInDomains(
            directory = NSDocumentDirectory,
            domainMask = NSUserDomainMask,
            expandTilde = true,
        )
        val basePath = (docs.firstOrNull() as? String) ?: NSTemporaryDirectory()
        "$basePath/axiomnode.db"
    }

    return Room.databaseBuilder<AxiomNodeDatabase>(
        name = dbPath,
    )
        .setDriver(BundledSQLiteDriver())
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
}

