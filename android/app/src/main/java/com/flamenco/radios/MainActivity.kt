package com.flamenco.radios

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flamenco.radios.ui.PlayerScreen
import com.flamenco.radios.ui.RadioListScreen
import com.flamenco.radios.ui.RadiosViewModel
import com.flamenco.radios.ui.theme.FlamencoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlamencoTheme {
                RequestNotificationPermission()
                FlamencoRadiosApp()
            }
        }
    }
}

@Composable
private fun FlamencoRadiosApp(viewModel: RadiosViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPlayer by rememberSaveable { mutableStateOf(false) }

    Box {
        RadioListScreen(
            state = state,
            onQueryChange = viewModel::onQueryChange,
            onStationClick = viewModel::onStationClick,
            onToggleFavorite = viewModel::toggleFavorite,
            onTogglePlayPause = viewModel::togglePlayPause,
            onOpenPlayer = { showPlayer = true },
        )

        val station = state.currentStation
        AnimatedVisibility(
            visible = showPlayer && station != null,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            if (station != null) {
                BackHandler { showPlayer = false }
                PlayerScreen(
                    station = station,
                    state = state,
                    onClose = { showPlayer = false },
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onStop = viewModel::stop,
                    onToggleFavorite = { viewModel.toggleFavorite(station) },
                    onVolumeChange = viewModel::setVolume,
                )
            }
        }
    }
}

/** Android 13+ : la notification média nécessite la permission POST_NOTIFICATIONS. */
@Composable
private fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
