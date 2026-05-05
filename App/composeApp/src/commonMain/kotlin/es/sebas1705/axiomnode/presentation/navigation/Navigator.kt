package es.sebas1705.axiomnode.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * A tiny in-memory navigator backed by a stack of [Destination]s.
 * Avoids pulling Navigation Compose KMP for an MVP while still providing
 * push / pop / replaceTopTab semantics shared across Android, iOS and Desktop.
 */
@Stable
class Navigator internal constructor(initial: Destination) {
    private val backStack = mutableStateListOf(initial)
    private val currentTabState = mutableStateOf(
        (initial as? Destination.Top)?.tab ?: TopDestination.HOME,
    )

    val current: Destination get() = backStack.last()
    val canGoBack: Boolean get() = backStack.size > 1
    val currentTab: TopDestination get() = currentTabState.value

    fun push(destination: Destination) {
        if (destination == current) return
        backStack.add(destination)
    }

    fun pop(): Boolean {
        if (!canGoBack) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }

    fun popToRoot() {
        while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    fun selectTab(tab: TopDestination) {
        currentTabState.value = tab
        // Reset the stack to only the chosen tab so that going "back" never
        // revisits stale sub-screens of another tab.
        backStack.clear()
        backStack.add(Destination.Top(tab))
    }
}

@Composable
fun rememberNavigator(initial: Destination = Destination.Top(TopDestination.HOME)): Navigator =
    remember { Navigator(initial) }

