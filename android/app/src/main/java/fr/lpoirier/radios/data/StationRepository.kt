package fr.lpoirier.radios.data

import android.content.Context
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.json.Json

/**
 * Lit la configuration embarquée dans les assets (copiée depuis `config/` par la tâche
 * Gradle `copyRadioConfig`), traduite dans la langue de l'app (`config/i18n/<langue>.json`).
 * Chaque langue est parsée une seule fois ; changer de langue ne relit pas `radios.json`.
 */
class StationRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private val source: RadioConfig by lazy {
        context.assets.open(ASSET_NAME).bufferedReader().use { reader ->
            json.decodeFromString(RadioConfig.serializer(), reader.readText())
        }
    }

    private val byLanguage = ConcurrentHashMap<String, RadioConfig>()

    /** Langue courante de l'app (celle du téléphone, ou choisie pour l'app sur Android 13+). */
    val language: String get() = appLanguage(context.resources.configuration.locales[0].language)

    val config: RadioConfig get() = config(language)

    fun config(language: String): RadioConfig = byLanguage.getOrPut(language) {
        source.translated(loadTranslations(language))
    }

    val stations: List<Station> get() = config.stations

    fun station(id: String): Station? = stations.firstOrNull { it.id == id }

    val genres: Map<String, Genre> get() = config.genres

    fun genre(id: String): Genre? = config.genres[id]

    fun categoryLabel(category: String): String =
        config.genres.values.firstNotNullOfOrNull { it.categories[category] } ?: category

    private fun loadTranslations(language: String): Translations? = runCatching {
        context.assets.open("$I18N_DIR/$language.json").bufferedReader().use { reader ->
            json.decodeFromString(Translations.serializer(), reader.readText())
        }
    }.getOrNull()

    private companion object {
        const val ASSET_NAME = "radios.json"
        const val I18N_DIR = "i18n"
    }
}
