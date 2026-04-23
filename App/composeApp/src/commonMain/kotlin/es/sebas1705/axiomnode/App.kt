package es.sebas1705.axiomnode

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import es.sebas1705.axiomnode.di.dataModule
import es.sebas1705.axiomnode.di.platformModule
import es.sebas1705.axiomnode.domain.models.Game
import es.sebas1705.axiomnode.presentation.auth.AuthScreen
import es.sebas1705.axiomnode.presentation.auth.AuthViewModel
import es.sebas1705.axiomnode.presentation.gameplay.GamePlayScreen
import es.sebas1705.axiomnode.presentation.gameplay.GamePlayViewModel
import es.sebas1705.axiomnode.presentation.games.GamesScreen
import es.sebas1705.axiomnode.presentation.games.GamesViewModel
import es.sebas1705.axiomnode.presentation.loading.LoadingScreen
import es.sebas1705.axiomnode.presentation.profile.ProfileScreen
import es.sebas1705.axiomnode.ui.AppTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.KoinAppDeclaration

// ─────────────────────────────────────────────────────────────────────────────
// Navigation destinations
// ─────────────────────────────────────────────────────────────────────────────
enum class AppScreen {
    LOADING,
    AUTH,
    MAIN,
}

enum class MainTab(val label: String, val icon: String) {
    HOME("Inicio", "🏠"),
    PROFILE("Perfil", "👤"),
}

// ─────────────────────────────────────────────────────────────────────────────
// Root composable
// ─────────────────────────────────────────────────────────────────────────────
@Composable
@Suppress("ModifierTopMost", "DEPRECATION")
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
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                AppNavigation(modifier)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Navigation host
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val authViewModel: AuthViewModel = koinInject()
    val gamesViewModel: GamesViewModel = koinInject()
    val authState by authViewModel.state.collectAsStateWithLifecycle()

    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.LOADING) }
    var splashCompleted by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1200)
        splashCompleted = true
    }

    LaunchedEffect(splashCompleted, authState.isAuthenticated) {
        if (!splashCompleted) return@LaunchedEffect
        currentScreen = if (authState.isAuthenticated) AppScreen.MAIN else AppScreen.AUTH
    }

    Crossfade(
        targetState = currentScreen,
        animationSpec = tween(durationMillis = 400),
        modifier = modifier,
        label = "screen-crossfade",
    ) { screen ->
        when (screen) {
            AppScreen.LOADING -> {
                LoadingScreen()
            }

            AppScreen.AUTH -> {
                AuthScreen(
                    viewModel = authViewModel,
                    onSignInSuccess = { currentScreen = AppScreen.MAIN },
                )
            }

            AppScreen.MAIN -> {
                MainScaffold(
                    authViewModel = authViewModel,
                    gamesViewModel = gamesViewModel,
                    onSignOut = { currentScreen = AppScreen.AUTH },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main scaffold with bottom navigation + gameplay overlay
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MainScaffold(
    authViewModel: AuthViewModel,
    gamesViewModel: GamesViewModel,
    onSignOut: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    var activeGame by remember { mutableStateOf<Game?>(null) }

    activeGame?.let { game ->
        val gamePlayViewModel: GamePlayViewModel = koinInject()
        GamePlayScreen(
            game = game,
            viewModel = gamePlayViewModel,
            onExit = { activeGame = null },
        )
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Text(
                                text = tab.icon,
                                style = MaterialTheme.typography.titleLarge,
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        Crossfade(
            targetState = selectedTab,
            animationSpec = tween(durationMillis = 300),
            modifier = Modifier.padding(innerPadding),
            label = "tab-crossfade",
        ) { tab ->
            when (tab) {
                MainTab.HOME -> {
                    GamesScreen(
                        screenTitle = "Inicio",
                        viewModel = gamesViewModel,
                        onGameSelected = { gameId ->
                            gamesViewModel.resolveGameForPlay(gameId) { resolved ->
                                activeGame = resolved
                            }
                        },
                    )
                }

                MainTab.PROFILE -> {
                    ProfileScreen(
                        authViewModel = authViewModel,
                        onSignOut = onSignOut,
                    )
                }
            }
        }
    }
}

