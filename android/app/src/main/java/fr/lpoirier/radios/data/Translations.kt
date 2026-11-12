package fr.lpoirier.radios.data

import kotlinx.serialization.Serializable

/**
 * Traduction d'une langue (`config/i18n/<langue>.json`) : surcharge les textes de `radios.json`.
 * Tout champ absent garde le texte d'origine de `radios.json` (en français, sauf le genre « cultura »).
 */
@Serializable
data class Translations(
    val language: String,
    val genres: Map<String, GenreText> = emptyMap(),
    val stations: Map<String, StationText> = emptyMap(),
)

@Serializable
data class GenreText(
    val name: String? = null,
    val description: String? = null,
    val categories: Map<String, String> = emptyMap(),
)

@Serializable
data class StationText(
    val description: String? = null,
    val region: String? = null,
)

/** Langues de l'app ; toute autre langue du téléphone retombe sur l'anglais (comme `res/values`). */
val SUPPORTED_LANGUAGES = listOf("en", "fr", "es")

fun appLanguage(systemLanguage: String): String =
    systemLanguage.takeIf { it in SUPPORTED_LANGUAGES } ?: "en"

/** Applique [t] à la configuration : genres, catégories, descriptions et régions traduits. */
fun RadioConfig.translated(t: Translations?): RadioConfig {
    if (t == null) return this
    return copy(
        genres = genres.mapValues { (id, genre) ->
            val text = t.genres[id] ?: return@mapValues genre
            genre.copy(
                name = text.name ?: genre.name,
                description = text.description ?: genre.description,
                categories = genre.categories.mapValues { (categoryId, label) -> text.categories[categoryId] ?: label },
            )
        },
        stations = stations.map { station ->
            val text = t.stations[station.id] ?: return@map station
            station.copy(
                description = text.description ?: station.description,
                region = text.region ?: station.region,
            )
        },
    )
}
