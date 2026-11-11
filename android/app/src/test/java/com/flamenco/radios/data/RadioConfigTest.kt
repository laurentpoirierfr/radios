package com.flamenco.radios.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.util.Base64

/**
 * Valide `config/radios.json` côté JVM (sans émulateur) : le fichier doit
 * être parsable par l'app et cohérent avec ce que l'UI et le lecteur attendent.
 */
class RadioConfigTest {

    companion object {
        private lateinit var config: RadioConfig

        @JvmStatic
        @BeforeClass
        fun load() {
            val path = System.getProperty("radios.config") ?: "../../../config/radios.json"
            val json = Json { ignoreUnknownKeys = true }
            config = json.decodeFromString(RadioConfig.serializer(), File(path).readText())
        }

        private val PNG = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte())
        private val RIFF = "RIFF".toByteArray()
    }

    @Test
    fun `le genre flamenco contient les 14 radios d'origine`() {
        assertEquals(14, config.stations.count { it.genre == "flamenco" })
    }

    @Test
    fun `chaque genre a au moins une station et une couleur valide`() {
        config.genres.forEach { (id, genre) ->
            assertTrue("genre $id sans station", config.stations.any { it.genre == id })
            assertTrue("genre $id : couleur ${genre.color}", genre.color.matches(Regex("^#[0-9A-Fa-f]{6}$")))
        }
    }

    @Test
    fun `les identifiants de categorie sont uniques tous genres confondus`() {
        val ids = config.genres.values.flatMap { it.categories.keys }
        assertEquals("catégories dupliquées : $ids", ids.size, ids.toSet().size)
    }

    @Test
    fun `les identifiants sont uniques et au format slug`() {
        val ids = config.stations.map { it.id }
        assertEquals("ids dupliqués : $ids", ids.size, ids.toSet().size)
        ids.forEach { assertTrue("slug invalide : $it", it.matches(Regex("^[a-z0-9]+(-[a-z0-9]+)*$"))) }
    }

    @Test
    fun `chaque station a un genre declare et une categorie de ce genre`() {
        config.stations.forEach {
            val genre = config.genres[it.genre]
            assertTrue("${it.id} : genre inconnu ${it.genre}", genre != null)
            assertTrue(
                "${it.id} : catégorie ${it.category} absente du genre ${it.genre}",
                it.category in genre!!.categories,
            )
        }
    }

    @Test
    fun `les flux sont des URL http ou https`() {
        config.stations.forEach {
            assertTrue("${it.id} : flux invalide ${it.stream}", it.stream.matches(Regex("^https?://.+")))
        }
    }

    @Test
    fun `les logos sont des data URI PNG ou WebP decodables`() {
        config.stations.forEach { station ->
            val match = Regex("^data:image/(png|webp);base64,(.+)$").find(station.logo)
            assertTrue("${station.id} : logo n'est pas une data URI", match != null)
            val (format, payload) = match!!.destructured
            val bytes = Base64.getDecoder().decode(payload)
            val magic = if (format == "png") PNG else RIFF
            assertTrue(
                "${station.id} : en-tête $format invalide",
                bytes.size > magic.size && bytes.copyOfRange(0, magic.size).contentEquals(magic),
            )
        }
    }

    @Test
    fun `les debits sont dans les bornes du schema`() {
        config.stations.forEach {
            assertTrue("${it.id} : débit ${it.bitrateKbps}", it.bitrateKbps in 16..320)
        }
    }
}
