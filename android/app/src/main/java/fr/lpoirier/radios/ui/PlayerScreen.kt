package fr.lpoirier.radios.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.lpoirier.radios.R
import fr.lpoirier.radios.data.Station

/** Écran lecteur plein écran : logo, titre ICY, lecture/pause/stop, volume, favori. */
@Composable
fun PlayerScreen(
    station: Station,
    state: RadiosUiState,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    onToggleFavorite: () -> Unit,
    onVolumeChange: (Float) -> Unit,
) {
    val player = state.player
    val isThisStation = player.stationId == station.id
    val active = isThisStation && (player.isPlaying || player.isBuffering)

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onClose) {
                    Icon(painterResource(R.drawable.ic_expand_more), stringResource(R.string.close_player))
                }
                FavoriteButton(isFavorite = station.id in state.favorites, onClick = onToggleFavorite)
            }

            Spacer(Modifier.height(24.dp))
            StationLogo(station, size = 240.dp)
            Spacer(Modifier.height(24.dp))

            Text(
                station.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                station.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))
            val status = when {
                isThisStation && player.error != null -> stringResource(R.string.playback_error, player.error)
                isThisStation && player.isBuffering -> stringResource(R.string.buffering)
                isThisStation && player.nowPlaying != null -> "♪ ${player.nowPlaying}"
                isThisStation && player.isPlaying -> stringResource(R.string.live)
                else -> null
            }
            if (status != null) {
                AssistChip(onClick = {}, label = { Text(status, textAlign = TextAlign.Center) })
            }

            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                IconButton(onClick = onStop, enabled = isThisStation) {
                    Icon(painterResource(R.drawable.ic_stop), stringResource(R.string.stop))
                }
                FilledIconButton(onClick = onTogglePlayPause, modifier = Modifier.size(80.dp)) {
                    PlayPauseIcon(
                        isActive = active,
                        isBuffering = isThisStation && player.isBuffering,
                        iconSize = 40.dp,
                    )
                }
                Spacer(Modifier.size(48.dp))
            }

            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_volume), stringResource(R.string.volume))
                Slider(
                    value = player.volume,
                    onValueChange = onVolumeChange,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }

            Spacer(Modifier.height(24.dp))
            Text(station.description, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                station.tags.joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}
