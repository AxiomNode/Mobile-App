package es.sebas1705.axiomnode.di

import androidx.room.Room
import es.sebas1705.axiomnode.auth.GoogleSignInService
import es.sebas1705.axiomnode.config.AppConfig
import es.sebas1705.axiomnode.data.db.AxiomNodeDatabase
import es.sebas1705.axiomnode.domain.usecases.AuthUseCase
import es.sebas1705.axiomnode.domain.usecases.GamesUseCase
import es.sebas1705.axiomnode.presentation.auth.AuthViewModel
import es.sebas1705.axiomnode.presentation.gameplay.GamePlayViewModel
import es.sebas1705.axiomnode.presentation.games.GamesViewModel
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

        single { GoogleSignInService(get(), get<AppConfig>()) }
        viewModel { AuthViewModel(get<AuthUseCase>(), get<GoogleSignInService>()) }
        viewModel { GamesViewModel(get<GamesUseCase>()) }
        viewModel { GamePlayViewModel(get<GamesUseCase>()) }
    }