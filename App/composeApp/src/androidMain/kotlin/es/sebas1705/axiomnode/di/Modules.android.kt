package es.sebas1705.axiomnode.di

import es.sebas1705.axiomnode.domain.usecases.AuthUseCase
import es.sebas1705.axiomnode.domain.usecases.GamesUseCase
import es.sebas1705.axiomnode.presentation.auth.AuthViewModel
import es.sebas1705.axiomnode.presentation.games.GamesViewModel
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.androidx.viewmodel.dsl.viewModel

actual val platformModule: Module
    get() = module {
        viewModel { AuthViewModel(get<AuthUseCase>()) }
        viewModel { GamesViewModel(get<GamesUseCase>()) }
    }