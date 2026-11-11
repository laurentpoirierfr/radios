package com.flamenco.radios

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.flamenco.radios.data.StationRepository
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URL

/**
 * Tests réseau (nécessitent Internet sur l'appareil/émulateur).
 *
 * Ils passent par la pile TLS d'Android et donc par
 * `network_security_config.xml` : c'est ici qu'on vérifie que le certificat
 * Sectigo embarqué débloque bien onda-sevilla et onda-mencia, et que le HTTP
 * clair est autorisé.
 */
@RunWith(AndroidJUnit4::class)
class StreamReachabilityTest {

    private val repo = StationRepository(InstrumentationRegistry.getInstrumentation().targetContext)

    @Test
    fun elitecomunicacionStreamsPassTls() {
        listOf("onda-sevilla", "onda-mencia").forEach { id -> assertStreamResponds(id) }
    }

    @Test
    fun allStreamsRespond() {
        val failures = repo.stations.mapNotNull { station ->
            runCatching { assertStreamResponds(station.id) }.exceptionOrNull()?.let { "${station.id}: ${it.message}" }
        }
        assertTrue("Flux en échec :\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    private fun assertStreamResponds(id: String) {
        val station = requireNotNull(repo.station(id)) { "station inconnue $id" }
        val conn = URL(station.stream).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("Icy-MetaData", "1")
            conn.setRequestProperty("User-Agent", "FlamencoRadios-Test")
            val code = conn.responseCode
            assertTrue("$id : HTTP $code", code in 200..299)
            // Lire quelques octets prouve que de l'audio arrive réellement.
            val read = conn.inputStream.use { it.read(ByteArray(4096)) }
            assertTrue("$id : aucun octet reçu", read > 0)
        } finally {
            conn.disconnect()
        }
    }
}
