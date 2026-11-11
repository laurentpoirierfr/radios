package com.flamenco.radios.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "preferences")

/** Favoris et dernière radio écoutée (DataStore Preferences). */
class PreferencesRepository(private val context: Context) {

    val favorites: Flow<Set<String>> =
        context.dataStore.data.map { it[FAVORITES] ?: emptySet() }

    val lastStationId: Flow<String?> =
        context.dataStore.data.map { it[LAST_STATION] }

    suspend fun lastStationIdNow(): String? = lastStationId.first()

    /** Genre choisi dans le sélecteur ; `null` = tous les genres. */
    val selectedGenre: Flow<String?> =
        context.dataStore.data.map { it[SELECTED_GENRE] }

    suspend fun setSelectedGenre(genreId: String?) {
        context.dataStore.edit { prefs ->
            if (genreId == null) prefs.remove(SELECTED_GENRE) else prefs[SELECTED_GENRE] = genreId
        }
    }

    suspend fun toggleFavorite(stationId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[FAVORITES] ?: emptySet()
            prefs[FAVORITES] = if (stationId in current) current - stationId else current + stationId
        }
    }

    suspend fun setLastStation(stationId: String) {
        context.dataStore.edit { it[LAST_STATION] = stationId }
    }

    private companion object {
        val FAVORITES = stringSetPreferencesKey("favorites")
        val LAST_STATION = stringPreferencesKey("last_station")
        val SELECTED_GENRE = stringPreferencesKey("selected_genre")
    }
}
