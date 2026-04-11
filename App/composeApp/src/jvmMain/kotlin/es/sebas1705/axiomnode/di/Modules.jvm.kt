package es.sebas1705.axiomnode.di

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import es.sebas1705.axiomnode.auth.GoogleSignInService
import es.sebas1705.axiomnode.config.AppConfig
import es.sebas1705.axiomnode.data.db.AxiomNodeDatabase
import es.sebas1705.axiomnode.domain.usecases.AuthUseCase
import es.sebas1705.axiomnode.domain.usecases.GamesUseCase
import es.sebas1705.axiomnode.presentation.auth.AuthViewModel
import es.sebas1705.axiomnode.presentation.gameplay.GamePlayViewModel
import es.sebas1705.axiomnode.presentation.games.GamesViewModel
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

actual val platformModule: Module
    get() = module {
        // Database (JVM uses bundled SQLite driver)
        single<AxiomNodeDatabase> {
            val dbDir = File(System.getProperty("user.home"), ".axiomnode")
            dbDir.mkdirs()
            val dbPath = File(dbDir, "axiomnode.db").absolutePath

            Room.databaseBuilder<AxiomNodeDatabase>(name = dbPath)
                .setDriver(BundledSQLiteDriver())
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }

        single { GoogleSignInService(get<AppConfig>()) }
        factory { AuthViewModel(get<AuthUseCase>(), get<GoogleSignInService>()) }
        factory { GamesViewModel(get<GamesUseCase>()) }
        factory { GamePlayViewModel(get<GamesUseCase>()) }
    }