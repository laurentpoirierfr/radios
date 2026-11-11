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
    }
}
