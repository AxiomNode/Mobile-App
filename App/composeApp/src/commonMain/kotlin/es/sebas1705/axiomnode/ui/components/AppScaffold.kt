package es.sebas1705.axiomnode.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.only
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.sebas1705.axiomnode.ui.layout.LocalWindowSize
import es.sebas1705.axiomnode.ui.layout.WindowSize

/**
 * Standard scaffold used by feature screens. Provides a Material3 [LargeTopAppBar]
 * (or compact [TopAppBar] when `large = false`), back navigation when [onBack] is
 * provided, and a snackbar host.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
    large: Boolean = true,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (innerPadding: androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    val windowSize = LocalWindowSize.current
    val maxContentWidth = when (windowSize) {
        WindowSize.COMPACT -> 600.dp
        WindowSize.MEDIUM -> 920.dp
        WindowSize.EXPANDED -> 1200.dp
    }
    val sidePadding = when (windowSize) {
        WindowSize.COMPACT -> 0.dp
        WindowSize.MEDIUM -> 8.dp
        WindowSize.EXPANDED -> 12.dp
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Top + WindowInsetsSides.Bottom,
        ),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = bottomBar,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            val titleNode: @Composable () -> Unit = {
                Text(
                    text = title,
                    style = if (large) {
                        MaterialTheme.typography.titleLarge
                    } else {
                        MaterialTheme.typography.titleMedium
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val nav: @Composable () -> Unit = {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                }
            }
            val act: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
                if (actions != null) actions()
            }
            val colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            )
            TopAppBar(
                title = titleNode,
                navigationIcon = nav,
                actions = act,
                colors = colors,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .fillMaxWidth()
                    .widthIn(max = maxContentWidth)
                    .padding(horizontal = sidePadding),
            ) {
                content(androidx.compose.foundation.layout.PaddingValues(0.dp))
            }
        }
    }
}

