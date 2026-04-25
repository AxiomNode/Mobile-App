package es.sebas1705.axiomnode.di

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import es.sebas1705.axiomnode.auth.GoogleSignInClient
import es.sebas1705.axiomnode.auth.GoogleSignInService
import es.sebas1705.axiomnode.config.AppConfig
import es.sebas1705.axiomnode.data.db.AxiomNodeDatabase
import es.sebas1705.axiomnode.domain.usecases.AuthUseCase
import es.sebas1705.axiomnode.domain.usecases.GamesUseCase
import es.sebas1705.axiomnode.presentation.auth.AuthViewModel
import es.sebas1705.axiomnode.presentation.gameplay.GamePlayViewModel
import es.sebas1705.axiomnode.presentation.games.GamesViewModel
import es.sebas1705.axiomnode.presentation.history.HistoryViewModel
import es.sebas1705.axiomnode.presentation.home.HomeViewModel
import es.sebas1705.axiomnode.presentation.settings.SettingsViewModel
import es.sebas1705.axiomnode.presentation.stats.StatsViewModel
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

        single<GoogleSignInClient> { GoogleSignInService(get<AppConfig>()) }
        factory { AuthViewModel(get<AuthUseCase>(), get<GoogleSignInClient>()) }
        factory { GamesViewModel(get<GamesUseCase>()) }
        factory { GamePlayViewModel(get<GamesUseCase>()) }
        factory { HomeViewModel(get<GamesUseCase>()) }
        factory { HistoryViewModel(get<GamesUseCase>()) }
        factory { StatsViewModel(get<GamesUseCase>()) }
        factory { SettingsViewModel(get(), get<GamesUseCase>()) }
    }