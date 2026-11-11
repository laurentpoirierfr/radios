package com.flamenco.radios.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flamenco.radios.app
import com.flamenco.radios.data.Station
import com.flamenco.radios.player.PlayerConnection
import com.flamenco.radios.player.PlayerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Normalizer

data class Section(val title: String, val stations: List<Station>)

data class RadiosUiState(
    val loading: Boolean = true,
    val sections: List<Section> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val query: String = "",
    /** Station affichée dans le mini-lecteur : en cours, sinon la dernière écoutée. */
    val currentStation: Station? = null,
    val player: PlayerState = PlayerState(),
)

class RadiosViewModel(application: Application) : AndroidViewModel(application) {

    private val stationsRepo = application.app.stations
    private val prefs = application.app.preferences
    private val player = PlayerConnection(application)

    private val query = MutableStateFlow("")

    private val allStations = flow { emit(stationsRepo.stations) }.flowOn(Dispatchers.IO)

    val uiState: StateFlow<RadiosUiState> = combine(
        allStations,
        prefs.favorites,
        prefs.lastStationId,
        query,
        player.state,
    ) { stations, favorites, lastId, q, playerState ->
        val currentId = playerState.stationId ?: lastId
        RadiosUiState(
            loading = false,
            sections = buildSections(stations, favorites, q),
            favorites = favorites,
            query = q,
            currentStation = stations.firstOrNull { it.id == currentId },
            player = playerState,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RadiosUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun toggleFavorite(station: Station) {
        viewModelScope.launch { prefs.toggleFavorite(station.id) }
    }

    fun play(station: Station) {
        player.play(station.id)
        viewModelScope.launch { prefs.setLastStation(station.id) }
    }

    /** Clic sur une radio : lance, ou met en pause si c'est déjà elle qui joue. */
    fun onStationClick(station: Station) {
        val state = uiState.value.player
        if (state.stationId == station.id && (state.isPlaying || state.isBuffering)) {
            player.pause()
        } else {
            play(station)
        }
    }

    fun togglePlayPause() {
        val state = uiState.value
        val station = state.currentStation ?: return
        if (state.player.isPlaying || state.player.isBuffering) player.pause() else play(station)
    }

    fun stop() = player.stop()

    fun setVolume(volume: Float) = player.setVolume(volume)

    override fun onCleared() {
        player.release()
    }

    private fun buildSections(stations: List<Station>, favorites: Set<String>, q: String): List<Section> {
        val needle = q.normalized()
        val filtered = if (needle.isEmpty()) stations else stations.filter { s ->
            val genreName = stationsRepo.genre(s.genre)?.name.orEmpty()
            listOf(s.name, s.region.orEmpty(), s.description, genreName, s.tags.joinToString(" "))
                .any { it.normalized().contains(needle) }
        }
        val sections = mutableListOf<Section>()
        val favs = filtered.filter { it.id in favorites }
        if (favs.isNotEmpty()) sections += Section(FAVORITES_TITLE, favs)
        // Genre puis catégorie, dans l'ordre déclaré par la configuration.
        stationsRepo.genres.values.forEach { genre ->
            genre.categories.forEach { (categoryId, label) ->
                val list = filtered.filter { it.category == categoryId }
                if (list.isNotEmpty()) sections += Section(label, list)
            }
        }
        return sections
    }

    companion object {
        const val FAVORITES_TITLE = "★ Favoris"

        /** Recherche insensible à la casse et aux accents (« radiole » trouve « Radiolé »). */
        private fun String.normalized(): String =
            Normalizer.normalize(trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
    }
}
