package es.sebas1705.axiomnode.di

import androidx.room.Room
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
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

actual val platformModule: Module
    get() = module {
        // Database (Android uses standard Room builder with Koin's androidContext)
        single<AxiomNodeDatabase> {
            Room.databaseBuilder(
                androidContext(),
                AxiomNodeDatabase::class.java,
                "axiomnode.db",
            )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }

        single<GoogleSignInClient> { GoogleSignInService(get(), get<AppConfig>()) }
        viewModel { AuthViewModel(get<AuthUseCase>(), get<GoogleSignInClient>()) }
        viewModel { GamesViewModel(get<GamesUseCase>()) }
        viewModel { GamePlayViewModel(get<GamesUseCase>()) }
        viewModel { HomeViewModel(get<GamesUseCase>()) }
        viewModel { HistoryViewModel(get<GamesUseCase>()) }
        viewModel { StatsViewModel(get<GamesUseCase>()) }
        viewModel { SettingsViewModel(get(), get<GamesUseCase>()) }
    }