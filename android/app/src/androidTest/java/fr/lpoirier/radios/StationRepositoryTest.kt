package fr.lpoirier.radios

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.lpoirier.radios.data.StationRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/** Vérifie sur l'appareil que la config est bien copiée dans les assets et décodable. */
@RunWith(AndroidJUnit4::class)
class StationRepositoryTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun configIsBundledInAssets() {
        val repo = StationRepository(context)
        assertEquals(14, repo.stations.count { it.genre == "flamenco" })
        assertNotNull(repo.station("canal-flamenco"))
        assertNotNull(repo.genre("flamenco"))
    }

    @Test
    fun logosDecodeWithAndroidBase64() {
        val repo = StationRepository(context)
        repo.stations.forEach { assertNotNull("logo ${it.id}", it.logoBytes()) }
    }
}
