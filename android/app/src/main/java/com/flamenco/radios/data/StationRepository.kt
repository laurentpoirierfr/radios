package com.flamenco.radios.data

import android.content.Context
import kotlinx.serialization.json.Json

/**
 * Lit la configuration embarquée dans les assets (copiée depuis `config/`
 * par la tâche Gradle `copyRadioConfig`). Le fichier est parsé une seule fois.
 */
class StationRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    val config: RadioConfig by lazy {
        context.assets.open(ASSET_NAME).bufferedReader().use { reader ->
            json.decodeFromString(RadioConfig.serializer(), reader.readText())
        }
    }

    val stations: List<Station> get() = config.stations

    fun station(id: String): Station? = stations.firstOrNull { it.id == id }

    val genres: Map<String, Genre> get() = config.genres

    fun genre(id: String): Genre? = config.genres[id]

    fun categoryLabel(category: String): String =
        config.genres.values.firstNotNullOfOrNull { it.categories[category] } ?: category

    private companion object {
        const val ASSET_NAME = "radios.json"
    }
}
