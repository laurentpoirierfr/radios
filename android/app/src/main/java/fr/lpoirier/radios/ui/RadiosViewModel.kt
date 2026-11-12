package fr.lpoirier.radios.ui

import fr.lpoirier.radios.R
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.lpoirier.radios.app
import fr.lpoirier.radios.data.Station
import fr.lpoirier.radios.player.PlayerConnection
import fr.lpoirier.radios.player.PlayerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Normalizer

/**
 * Bloc de la liste. [genreHeader] non nul : afficher le nom du genre avant ce bloc
 * (vue « Tous » ou recherche). [color] : couleur ARGB du genre (null pour les favoris).
 */
data class Section(
    val title: String,
    val stations: List<Station>,
    val color: Int? = null,
    val genreHeader: String? = null,
)

/** Rangée horizontale de la vue « Tous » : un genre (ou les favoris si [genreId] est nul). */
data class Carousel(
    val genreId: String?,
    val title: String,
    val color: Int?,
    val stations: List<Station>,
)

data class RadiosUiState(
    val loading: Boolean = true,
    /** Liste détaillée : vue d'un genre ou résultats de recherche. */
    val sections: List<Section> = emptyList(),
    /** Vue « Tous » sans recherche : favoris puis un carrousel par genre (sinon vide). */
    val carousels: List<Carousel> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val query: String = "",
    val genres: List<GenreOption> = emptyList(),
    /** Genre sélectionné ; `null` = tous. */
    val selectedGenre: String? = null,
    /** Station affichée dans le mini-lecteur : en cours, sinon la dernière écoutée. */
    val currentStation: Station? = null,
    val player: PlayerState = PlayerState(),
)

class RadiosViewModel(application: Application) : AndroidViewModel(application) {

    private val stationsRepo = application.app.stations
    private val prefs = application.app.preferences
    private val player = PlayerConnection(application)

    private val query = MutableStateFlow("")

    /** Titre de la section favoris, dans la langue de l'app. */
    private val favoritesTitle: String get() = "★ " + getApplication<Application>().getString(R.string.favorites)

    /** Langue de l'app : quand elle change, stations et genres sont relus traduits. */
    private val language = MutableStateFlow(stationsRepo.language)

    private val allStations = language.map { stationsRepo.config(it).stations }.flowOn(Dispatchers.IO)

    /** À appeler quand la configuration (langue) de l'activité change. */
    fun onLanguageChanged() {
        language.value = stationsRepo.language
    }

    private data class Prefs(val favorites: Set<String>, val lastId: String?, val genre: String?)

    private val prefsState = combine(prefs.favorites, prefs.lastStationId, prefs.selectedGenre, ::Prefs)

    val uiState: StateFlow<RadiosUiState> = combine(
        allStations,
        prefsState,
        query,
        player.state,
    ) { stations, p, q, playerState ->
        val currentId = playerState.stationId ?: p.lastId
        // Un genre retiré de la configuration ne doit pas laisser une liste vide.
        val selected = p.genre?.takeIf { it in stationsRepo.genres }
        RadiosUiState(
            loading = false,
            sections = if (showCarousels(q, selected)) emptyList() else buildSections(stations, p.favorites, q, selected),
            carousels = if (showCarousels(q, selected)) buildCarousels(stations, p.favorites) else emptyList(),
            favorites = p.favorites,
            query = q,
            genres = genreOptions,
            selectedGenre = selected,
            currentStation = stations.firstOrNull { it.id == currentId },
            player = playerState,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RadiosUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun selectGenre(genreId: String?) {
        viewModelScope.launch { prefs.setSelectedGenre(genreId) }
    }

    /** Couleur ARGB du genre d'une station (écran lecteur). */
    fun genreColor(station: Station): Int? = genreOptions.firstOrNull { it.id == station.genre }?.color

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

    private val genreOptions: List<GenreOption>
        get() = stationsRepo.genres.map { (id, genre) ->
            GenreOption(id, genre.name, runCatching { android.graphics.Color.parseColor(genre.color) }.getOrDefault(0))
        }

    private fun showCarousels(q: String, selectedGenre: String?) = selectedGenre == null && q.isBlank()

    /** Vue « Tous » : favoris (s'il y en a) puis un carrousel par genre, dans l'ordre de la configuration. */
    private fun buildCarousels(stations: List<Station>, favorites: Set<String>): List<Carousel> = buildList {
        val favs = stations.filter { it.id in favorites }
        if (favs.isNotEmpty()) add(Carousel(null, favoritesTitle, null, favs))
        genreOptions.forEach { genre ->
            val list = stations.filter { it.genre == genre.id }
            if (list.isNotEmpty()) add(Carousel(genre.id, genre.name, genre.color, list))
        }
    }

    /**
     * Sections affichées. Une recherche porte sur tous les genres (une radio n'est
     * jamais introuvable) ; sinon seul le genre sélectionné est affiché.
     */
    private fun buildSections(
        stations: List<Station>,
        favorites: Set<String>,
        q: String,
        selectedGenre: String?,
    ): List<Section> {
        val needle = q.normalized()
        val filtered = when {
            needle.isNotEmpty() -> stations.filter { s ->
                val genreName = stationsRepo.genre(s.genre)?.name.orEmpty()
                listOf(s.name, s.region.orEmpty(), s.description, genreName, s.tags.joinToString(" "))
                    .any { it.normalized().contains(needle) }
            }
            selectedGenre != null -> stations.filter { it.genre == selectedGenre }
            else -> stations
        }
        val sections = mutableListOf<Section>()
        val favs = filtered.filter { it.id in favorites }
        if (favs.isNotEmpty()) sections += Section(favoritesTitle, favs)

        // Plusieurs genres visibles : on affiche le nom du genre en tête de son bloc.
        val showGenreHeaders = filtered.map { it.genre }.distinct().size > 1
        // Genre puis catégorie, dans l'ordre déclaré par la configuration.
        stationsRepo.genres.forEach { (genreId, genre) ->
            val color = genreOptions.firstOrNull { it.id == genreId }?.color
            var first = true
            genre.categories.forEach { (categoryId, label) ->
                val list = filtered.filter { it.genre == genreId && it.category == categoryId }
                if (list.isNotEmpty()) {
                    sections += Section(label, list, color, genre.name.takeIf { showGenreHeaders && first })
                    first = false
                }
            }
        }
        return sections
    }

    companion object {
        /** Recherche insensible à la casse et aux accents (« radiole » trouve « Radiolé »). */
        private fun String.normalized(): String =
            Normalizer.normalize(trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
    }
}
