package fr.lpoirier.radios.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.lpoirier.radios.R
import fr.lpoirier.radios.data.Station
import fr.lpoirier.radios.player.PlayerState

private val TileWidth = 96.dp
private val TileLogo = 88.dp

/**
 * Bloc de la vue « Tous » : titre du genre (dans sa couleur), nombre de radios,
 * « Voir tout » (bascule sur le genre) et rangée de logos défilant horizontalement.
 */
@Composable
fun CarouselBlock(
    carousel: Carousel,
    genres: List<GenreOption>,
    player: PlayerState,
    onStationClick: (Station) -> Unit,
    onSeeAll: (String) -> Unit,
) {
    val accent = genreAccent(carousel.color)
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    carousel.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
                Text(
                    pluralStringResource(R.plurals.radio_count, carousel.stations.size, carousel.stations.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            carousel.genreId?.let { genreId ->
                TextButton(onClick = { onSeeAll(genreId) }) {
                    Text(stringResource(R.string.see_all), color = accent, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(carousel.stations, key = { it.id }) { station ->
                val isCurrent = player.stationId == station.id
                StationTile(
                    station = station,
                    isCurrent = isCurrent,
                    isActive = isCurrent && (player.isPlaying || player.isBuffering),
                    isBuffering = isCurrent && player.isBuffering,
                    // Couleur du genre de la radio (les favoris mélangent les genres).
                    accent = genreAccent(genres.firstOrNull { it.id == station.genre }?.color),
                    onClick = { onStationClick(station) },
                )
            }
        }
    }
}

/** Tuile : logo, nom sur deux lignes ; la radio en cours est cerclée et porte un badge lecture/pause. */
@Composable
private fun StationTile(
    station: Station,
    isCurrent: Boolean,
    isActive: Boolean,
    isBuffering: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(TileWidth)
            .clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            StationLogo(
                station = station,
                size = TileLogo,
                modifier = if (isCurrent) {
                    Modifier.border(BorderStroke(3.dp, accent), RoundedCornerShape(TileLogo / 6))
                } else {
                    Modifier
                },
            )
            if (isCurrent) {
                Surface(
                    shape = CircleShape,
                    color = accent,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(4.dp).size(30.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        PlayPauseIcon(isActive = isActive, isBuffering = isBuffering, iconSize = 18.dp)
                    }
                }
            }
        }
        Text(
            station.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
