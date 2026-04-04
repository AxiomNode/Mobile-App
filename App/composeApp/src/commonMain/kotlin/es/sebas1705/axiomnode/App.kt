package es.sebas1705.axiomnode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import es.sebas1705.axiomnode.di.dataModule
import es.sebas1705.axiomnode.di.platformModule
import es.sebas1705.axiomnode.presentation.auth.AuthScreen
import es.sebas1705.axiomnode.presentation.auth.AuthViewModel
import es.sebas1705.axiomnode.presentation.games.GamesScreen
import es.sebas1705.axiomnode.presentation.games.GamesViewModel
import es.sebas1705.axiomnode.ui.AppTheme
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.KoinAppDeclaration

@Composable
@Suppress("ModifierTopMost")
fun App(
    modifier: Modifier = Modifier,
    koinAppDeclaration: KoinAppDeclaration? = null,
) {
    KoinApplication(
        application = {
            koinAppDeclaration?.invoke(this)
            modules(dataModule, platformModule)
        }
    ) {
        AppTheme {
            AppNavigation(modifier)
        }
    }
}

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val authViewModel: AuthViewModel = koinInject()
    val gamesViewModel: GamesViewModel = koinInject()
    
    var showGames by remember { mutableStateOf(false) }

    if (showGames) {
        GamesScreen(
            viewModel = gamesViewModel,
            onGameSelected = { gameId ->
                // TODO: Navegar a pantalla de juego
            },
        )
    } else {
        AuthScreen(
            viewModel = authViewModel,
            onSignInSuccess = { showGames = true },
        )
    }
}
