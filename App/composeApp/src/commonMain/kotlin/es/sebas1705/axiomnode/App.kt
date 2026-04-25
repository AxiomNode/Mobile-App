package es.sebas1705.axiomnode

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.sebas1705.axiomnode.di.dataModule
import es.sebas1705.axiomnode.di.platformModule
import es.sebas1705.axiomnode.data.network.ConnectivityMonitor
import es.sebas1705.axiomnode.presentation.about.AboutScreen
import es.sebas1705.axiomnode.presentation.auth.AuthScreen
import es.sebas1705.axiomnode.presentation.auth.AuthViewModel
import es.sebas1705.axiomnode.presentation.catalog.CatalogScreen
import es.sebas1705.axiomnode.presentation.gameplay.GamePlayScreen
import es.sebas1705.axiomnode.presentation.gameplay.GamePlayViewModel
import es.sebas1705.axiomnode.presentation.games.GamesViewModel
import es.sebas1705.axiomnode.presentation.history.HistoryDetailScreen
import es.sebas1705.axiomnode.presentation.history.HistoryScreen
import es.sebas1705.axiomnode.presentation.history.HistoryViewModel
import es.sebas1705.axiomnode.presentation.home.HomeScreen
import es.sebas1705.axiomnode.presentation.home.HomeViewModel
import es.sebas1705.axiomnode.presentation.loading.LoadingScreen
import es.sebas1705.axiomnode.presentation.lobby.GameLobbyScreen
import es.sebas1705.axiomnode.presentation.navigation.Destination
import es.sebas1705.axiomnode.presentation.navigation.Navigator
import es.sebas1705.axiomnode.presentation.navigation.TopDestination
import es.sebas1705.axiomnode.presentation.navigation.rememberNavigator
import es.sebas1705.axiomnode.presentation.profile.ProfileScreen
import es.sebas1705.axiomnode.presentation.settings.SettingsScreen
import es.sebas1705.axiomnode.presentation.settings.SettingsViewModel
import es.sebas1705.axiomnode.presentation.stats.StatsScreen
import es.sebas1705.axiomnode.presentation.stats.StatsViewModel
import es.sebas1705.axiomnode.ui.AppTheme
import es.sebas1705.axiomnode.ui.layout.LocalWindowSize
import es.sebas1705.axiomnode.ui.layout.WindowSize
import es.sebas1705.axiomnode.ui.layout.windowSizeFor
import es.sebas1705.axiomnode.domain.usecases.AuthUseCase
import es.sebas1705.axiomnode.domain.usecases.GamesUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.KoinAppDeclaration

private enum class RootScreen { LOADING, AUTH, MAIN }

private const val FIRST_SYNC_RANDOM_DOWNLOAD_COUNT = 200

// ─────────────────────────────────────────────────────────────────────────────
// App entry-point
// ─────────────────────────────────────────────────────────────────────────────

@Composable
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
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val windowSize = windowSizeFor(maxWidth)
                CompositionLocalProvider(LocalWindowSize provides windowSize) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        AppRoot(modifier = modifier)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Root scene: splash → auth → main
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AppRoot(modifier: Modifier = Modifier) {
    val authViewModel: AuthViewModel = koinInject()
    val authUseCase: AuthUseCase = koinInject()
    val gamesUseCase: GamesUseCase = koinInject()
    val connectivityMonitor: ConnectivityMonitor = koinInject()
    val authState by authViewModel.state.collectAsStateWithLifecycle()

    var rootScreen by rememberSaveable { mutableStateOf(RootScreen.LOADING) }
    var splashCompleted by rememberSaveable { mutableStateOf(false) }
    var startupSyncCompleted by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val isOnline = runCatching { connectivityMonitor.refreshNow() }.getOrDefault(false)
        if (isOnline) {
            runCatching {
                coroutineScope {
                    val profileSync = async { authUseCase.getCurrentUser() }
                    val catalogSync = async { gamesUseCase.getGameCatalog() }
                    val firstContentSync = async {
                        gamesUseCase.getRandomGames(
                            count = FIRST_SYNC_RANDOM_DOWNLOAD_COUNT,
                            language = "es",
                        )
                    }
                    profileSync.await()
                    catalogSync.await()
                    firstContentSync.await()
                }
            }
        }
        startupSyncCompleted = true
    }

    LaunchedEffect(Unit) {
        delay(1200)
        splashCompleted = true
    }
    LaunchedEffect(splashCompleted, startupSyncCompleted, authState.isAuthenticated) {
        if (!splashCompleted || !startupSyncCompleted) return@LaunchedEffect
        rootScreen = if (authState.isAuthenticated) RootScreen.MAIN else RootScreen.AUTH
    }
    LaunchedEffect(rootScreen) {
        if (rootScreen == RootScreen.MAIN) {
            authViewModel.syncProfileOnEnter()
        }
    }

    AnimatedContent(
        targetState = rootScreen,
        transitionSpec = { fadeIn(tween(300)).togetherWith(fadeOut(tween(300))) },
        modifier = modifier,
        label = "root-scene",
    ) { screen ->
        when (screen) {
            RootScreen.LOADING -> LoadingScreen()
            RootScreen.AUTH -> AuthScreen(
                viewModel = authViewModel,
                onSignInSuccess = { rootScreen = RootScreen.MAIN },
            )
            RootScreen.MAIN -> MainScene(
                authViewModel = authViewModel,
                onSignOut = { rootScreen = RootScreen.AUTH },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main scene with Navigator + adaptive bottom bar / nav rail
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MainScene(
    authViewModel: AuthViewModel,
    onSignOut: () -> Unit,
) {
    val navigator = rememberNavigator()
    val windowSize = LocalWindowSize.current

    val tabsVisible = navigator.current is Destination.Top

    if (windowSize == WindowSize.COMPACT) {
        // Phone layout
        androidx.compose.material3.Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (tabsVisible) {
                    BottomTabs(
                        selected = navigator.currentTab,
                        onSelect = navigator::selectTab,
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            DestinationContent(
                navigator = navigator,
                authViewModel = authViewModel,
                onSignOut = onSignOut,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        }
    } else {
        // Tablet / desktop layout: nav rail on the side
        Row(modifier = Modifier.fillMaxSize()) {
            if (tabsVisible) {
                SideRail(
                    selected = navigator.currentTab,
                    onSelect = navigator::selectTab,
                )
            }
            DestinationContent(
                navigator = navigator,
                authViewModel = authViewModel,
                onSignOut = onSignOut,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun DestinationContent(
    navigator: Navigator,
    authViewModel: AuthViewModel,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = navigator.current,
        transitionSpec = { fadeIn(tween(220)).togetherWith(fadeOut(tween(220))) },
        modifier = modifier,
        label = "destination",
    ) { destination ->
        when (destination) {
            is Destination.Top -> when (destination.tab) {
                TopDestination.HOME -> {
                    val vm: HomeViewModel = koinInject()
                    HomeScreen(viewModel = vm, user = authState.user, navigator = navigator)
                }
                TopDestination.CATALOG -> {
                    val vm: GamesViewModel = koinInject()
                    CatalogScreen(viewModel = vm, navigator = navigator)
                }
                TopDestination.HISTORY -> {
                    val vm: HistoryViewModel = koinInject()
                    HistoryScreen(viewModel = vm, navigator = navigator)
                }
                TopDestination.STATS -> {
                    val vm: StatsViewModel = koinInject()
                    StatsScreen(viewModel = vm, navigator = navigator)
                }
                TopDestination.PROFILE -> {
                    ProfileScreen(
                        authViewModel = authViewModel,
                        navigator = navigator,
                        onSignOut = onSignOut,
                    )
                }
            }
            is Destination.Lobby -> {
                val vm: GamesViewModel = koinInject()
                GameLobbyScreen(
                    gameType = destination.gameType,
                    viewModel = vm,
                    navigator = navigator,
                )
            }
            is Destination.Play -> {
                val gamesVm: GamesViewModel = koinInject()
                val playVm: GamePlayViewModel = koinInject()
                var resolved by remember { mutableStateOf<es.sebas1705.axiomnode.domain.models.Game?>(null) }
                LaunchedEffect(destination.gameId) {
                    gamesVm.resolveGameForPlay(destination.gameId) { resolved = it }
                }
                resolved?.let { game ->
                    GamePlayScreen(
                        game = game,
                        viewModel = playVm,
                        onExit = { navigator.popToRoot() },
                    )
                } ?: LoadingScreen()
            }
            is Destination.HistoryDetail -> HistoryDetailScreen(
                gameId = destination.gameId,
                navigator = navigator,
            )
            Destination.Settings -> {
                val vm: SettingsViewModel = koinInject()
                SettingsScreen(viewModel = vm, navigator = navigator)
            }
            Destination.About -> AboutScreen(navigator = navigator)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom navigation bar / Side rail
// ─────────────────────────────────────────────────────────────────────────────

private fun TopDestination.icon(): ImageVector = when (this) {
    TopDestination.HOME -> Icons.Outlined.Home
    TopDestination.CATALOG -> Icons.Outlined.SportsEsports
    TopDestination.HISTORY -> Icons.Outlined.History
    TopDestination.STATS -> Icons.Outlined.BarChart
    TopDestination.PROFILE -> Icons.Outlined.Person
}

@Composable
private fun BottomTabs(
    selected: TopDestination,
    onSelect: (TopDestination) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        windowInsets = NavigationBarDefaults.windowInsets,
    ) {
        TopDestination.entries.forEach { tab ->
            NavigationBarItem(
                selected = selected == tab,
                onClick = { onSelect(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon(),
                        contentDescription = tab.title,
                    )
                },
                label = { Text(tab.title, style = MaterialTheme.typography.labelSmall) },
            )
        }
    }
}

@Composable
private fun SideRail(
    selected: TopDestination,
    onSelect: (TopDestination) -> Unit,
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        windowInsets = NavigationRailDefaults.windowInsets,
    ) {
        TopDestination.entries.forEach { tab ->
            NavigationRailItem(
                selected = selected == tab,
                onClick = { onSelect(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon(),
                        contentDescription = tab.title,
                    )
                },
                label = { Text(tab.title, style = MaterialTheme.typography.labelSmall) },
            )
        }
    }
}




