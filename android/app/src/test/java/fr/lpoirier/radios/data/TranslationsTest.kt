package fr.lpoirier.radios.data

import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/**
 * Valide les traductions `config/i18n/<langue>.json` contre `config/radios.json` :
 * - aucun identifiant inconnu (faute de frappe, radio retirée) ;
 * - l'anglais (langue par défaut) traduit tout : chaque nouvelle radio doit y être ajoutée.
 */
class TranslationsTest {

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        private lateinit var config: RadioConfig
        private lateinit var translations: Map<String, Translations>

        @JvmStatic
        @BeforeClass
        fun load() {
            val configPath = System.getProperty("radios.config") ?: "../../../config/radios.json"
            val i18nPath = System.getProperty("radios.i18n") ?: "../../../config/i18n"
            config = json.decodeFromString(RadioConfig.serializer(), File(configPath).readText())
            translations = SUPPORTED_LANGUAGES.associateWith { lang ->
                json.decodeFromString(Translations.serializer(), File(i18nPath, "$lang.json").readText())
            }
        }
    }

    @Test
    fun `chaque langue de l'app a son fichier, avec la bonne langue declaree`() {
        translations.forEach { (lang, t) -> assertEquals("$lang.json", lang, t.language) }
    }

    @Test
    fun `aucun identifiant inconnu dans les traductions`() {
        val stationIds = config.stations.map { it.id }.toSet()
        translations.forEach { (lang, t) ->
            t.genres.forEach { (genreId, text) ->
                val genre = config.genres[genreId]
                assertTrue("$lang.json : genre inconnu « $genreId »", genre != null)
                text.categories.keys.forEach { categoryId ->
                    assertTrue(
                        "$lang.json : catégorie inconnue « $categoryId » dans « $genreId »",
                        categoryId in genre!!.categories,
                    )
                }
            }
            t.stations.keys.forEach { id -> assertTrue("$lang.json : radio inconnue « $id »", id in stationIds) }
        }
    }

    @Test
    fun `l'anglais traduit tous les genres, categories et radios`() {
        val en = translations.getValue("en")
        val missing = buildList {
            config.genres.forEach { (id, genre) ->
                val text = en.genres[id]
                if (text?.name == null) add("genre $id : name")
                if (text?.description == null) add("genre $id : description")
                genre.categories.keys.filter { it !in text?.categories.orEmpty() }.forEach { add("catégorie $id/$it") }
            }
            config.stations.forEach { s ->
                val text = en.stations[s.id]
                if (text?.description == null) add("radio ${s.id} : description")
                if (s.region != null && text?.region == null) add("radio ${s.id} : region")
            }
        }
        assertTrue("Traductions anglaises manquantes dans config/i18n/en.json :\n" + missing.joinToString("\n"), missing.isEmpty())
    }

    @Test
    fun `la traduction remplace les textes et garde l'origine pour le reste`() {
        val es = config.translated(translations.getValue("es"))
        assertEquals("Clásica", es.genres.getValue("classique").name)
        assertEquals(config.stations.size, es.stations.size)
        // « cultura » est déjà en espagnol dans radios.json : la description d'origine est conservée.
        assertEquals(config.genres.getValue("cultura").description, es.genres.getValue("cultura").description)
        assertEquals(config, config.translated(null))
    }

    @Test
    fun `langue non prise en charge retombe sur l'anglais`() {
        assertEquals("en", appLanguage("de"))
        assertEquals("es", appLanguage("es"))
    }
}
