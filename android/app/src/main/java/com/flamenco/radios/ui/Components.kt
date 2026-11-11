package com.flamenco.radios.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.flamenco.radios.R
import com.flamenco.radios.data.Station

/** Logo de la station, décodé par Coil directement depuis la data URI base64. */
@Composable
fun StationLogo(station: Station, size: Dp, modifier: Modifier = Modifier) {
    AsyncImage(
        model = station.logo,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 6))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

@Composable
fun FavoriteButton(isFavorite: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            painter = painterResource(if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border),
            contentDescription = stringResource(if (isFavorite) R.string.remove_favorite else R.string.add_favorite),
            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Bouton lecture/pause avec indicateur de connexion. */
@Composable
fun PlayPauseIcon(isActive: Boolean, isBuffering: Boolean, iconSize: Dp = 24.dp) {
    Box(contentAlignment = Alignment.Center) {
        if (isBuffering) {
            CircularProgressIndicator(modifier = Modifier.size(iconSize + 12.dp), strokeWidth = 2.dp)
        }
        Icon(
            painter = painterResource(if (isActive) R.drawable.ic_pause else R.drawable.ic_play),
            contentDescription = stringResource(if (isActive) R.string.pause else R.string.play),
            modifier = Modifier.size(iconSize),
        )
    }
}
