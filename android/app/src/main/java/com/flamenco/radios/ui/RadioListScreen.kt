package com.flamenco.radios.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flamenco.radios.R
import com.flamenco.radios.data.Station

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioListScreen(
    state: RadiosUiState,
    onQueryChange: (String) -> Unit,
    onStationClick: (Station) -> Unit,
    onToggleFavorite: (Station) -> Unit,
    onTogglePlayPause: () -> Unit,
    onOpenPlayer: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.logo_radio),
                            contentDescription = null, // décoratif : le titre suit
                            modifier = Modifier.size(width = 30.dp, height = 36.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            state.currentStation?.let { station ->
                MiniPlayer(
                    station = station,
                    state = state,
                    onTogglePlayPause = onTogglePlayPause,
                    onClick = onOpenPlayer,
                )
            }
        },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        val listState = rememberLazyListState()
        Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 8.dp,
            ),
            // Marge à droite pour que l'ascenseur ne recouvre pas le bouton favori.
            modifier = Modifier.fillMaxSize().padding(end = 20.dp),
        ) {
            item(key = "search") {
                SearchField(
                    query = state.query,
                    onQueryChange = onQueryChange,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (state.sections.isEmpty()) {
                item(key = "empty") {
                    Text(
                        stringResource(R.string.no_result),
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            state.sections.forEach { section ->
                stickyHeader(key = "header-${section.title}") {
                    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            section.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                    }
                }
                items(section.stations, key = { "${section.title}-${it.id}" }) { station ->
                    StationRow(
                        station = station,
                        isFavorite = station.id in state.favorites,
                        isCurrent = state.player.stationId == station.id,
                        isActive = state.player.stationId == station.id &&
                            (state.player.isPlaying || state.player.isBuffering),
                        isBuffering = state.player.stationId == station.id && state.player.isBuffering,
                        onClick = { onStationClick(station) },
                        onToggleFavorite = { onToggleFavorite(station) },
                    )
                }
            }
        }
        VerticalScrollbar(
            state = listState,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = padding.calculateTopPadding() + 4.dp, bottom = padding.calculateBottomPadding() + 4.dp),
        )
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.clear_search))
                }
            }
        },
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun StationRow(
    station: Station,
    isFavorite: Boolean,
    isCurrent: Boolean,
    isActive: Boolean,
    isBuffering: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background,
        ),
        leadingContent = { StationLogo(station, size = 56.dp) },
        headlineContent = {
            Text(station.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
        },
        supportingContent = {
            Text(station.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isCurrent) PlayPauseIcon(isActive = isActive, isBuffering = isBuffering)
                FavoriteButton(isFavorite = isFavorite, onClick = onToggleFavorite)
            }
        },
    )
}

@Composable
private fun MiniPlayer(
    station: Station,
    state: RadiosUiState,
    onTogglePlayPause: () -> Unit,
    onClick: () -> Unit,
) {
    val player = state.player
    val active = player.stationId == station.id && (player.isPlaying || player.isBuffering)
    Surface(tonalElevation = 6.dp, shadowElevation = 6.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        ) {
            StationLogo(station, size = 44.dp)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(station.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text(
                    text = when {
                        player.error != null && player.stationId == station.id ->
                            stringResource(R.string.playback_error, player.error)
                        player.isBuffering && player.stationId == station.id -> stringResource(R.string.buffering)
                        player.nowPlaying != null && player.stationId == station.id -> player.nowPlaying
                        else -> station.subtitle
                    },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onTogglePlayPause) {
                PlayPauseIcon(isActive = active, isBuffering = player.isBuffering && player.stationId == station.id)
            }
        }
    }
}
