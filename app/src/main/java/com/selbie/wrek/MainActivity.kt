package com.selbie.wrek

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import com.selbie.wrek.data.repository.SettingsRepository
import com.selbie.wrek.data.repository.ShowRepository
import com.selbie.wrek.data.models.PlaybackState
import com.selbie.wrek.ui.components.AppDrawer
import com.selbie.wrek.ui.components.MediaFooter
import com.selbie.wrek.ui.components.ShowListItem
import com.selbie.wrek.ui.screens.AboutScreen
import com.selbie.wrek.ui.screens.SettingsScreen
import com.selbie.wrek.ui.theme.WrekTheme
import com.selbie.wrek.utils.NetworkMonitor
import com.selbie.wrek.viewmodels.MainViewModel
import com.selbie.wrek.viewmodels.PlaybackViewModel
import com.selbie.wrek.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch

/**
 * Main activity for WREK Online app
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize repositories
        val settingsRepository = SettingsRepository(applicationContext)
        val showRepository = ShowRepository()

        // Initialize network monitor
        val networkMonitor = NetworkMonitor(applicationContext)

        setContent {
            WrekTheme {
                // Create PlaybackViewModel
                val playbackViewModel: PlaybackViewModel = viewModel(
                    factory = PlaybackViewModel.Factory(
                        application,
                        showRepository,
                        settingsRepository,
                        networkMonitor
                    )
                )

                WrekApp(
                    settingsRepository = settingsRepository,
                    showRepository = showRepository,
                    playbackViewModel = playbackViewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrekApp(
    settingsRepository: SettingsRepository,
    showRepository: ShowRepository,
    playbackViewModel: PlaybackViewModel
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Track current route to enable/disable drawer
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val isMainScreen = currentRoute == "main"

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isMainScreen, // Only enable drawer gestures on main screen
        drawerContent = {
            AppDrawer(
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToAbout = { navController.navigate("about") },
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = "main",
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None }
        ) {
            composable("main") {
                val mainViewModel: MainViewModel = viewModel(
                    factory = MainViewModel.Factory(showRepository, settingsRepository)
                )
                MainScreen(
                    viewModel = mainViewModel,
                    playbackViewModel = playbackViewModel,
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }

            composable("settings") {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(settingsRepository)
                )
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("about") {
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    playbackViewModel: PlaybackViewModel,
    onOpenDrawer: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val playbackState by playbackViewModel.playbackState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontFamily = FontFamily(Font(R.font.metro_df))
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(R.string.nav_drawer_open)
                        )
                    }
                }
            )
        },
        bottomBar = {
            MediaFooter(
                playbackState = playbackState,
                onPlayPauseToggle = {
                    when (playbackState) {
                        is PlaybackState.Playing -> {
                            if ((playbackState as PlaybackState.Playing).isLiveStream) {
                                playbackViewModel.stop()
                            } else {
                                playbackViewModel.pause()
                            }
                        }
                        is PlaybackState.Paused -> playbackViewModel.resume()
                        else -> { /* no-op */ }
                    }
                },
                onStop = { playbackViewModel.stop() },
                onSeekTo = { positionMs -> playbackViewModel.seekTo(positionMs) }
            )
        }
    ) { paddingValues ->
        val errorMessage = uiState.errorMessage
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.retry() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.shows,
                        key = { show -> show.id }
                    ) { show ->
                        ShowListItem(
                            show = show,
                            isSelected = show.id == uiState.selectedShowId,
                            onClick = {
                                viewModel.selectShow(show.id)       // Update UI selection
                                playbackViewModel.play(show.id)      // Start playback
                            }
                        )
                    }
                }
            }
        }
    }
}