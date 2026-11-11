package fr.lpoirier.radios

import android.app.Application
import android.content.Context
import fr.lpoirier.radios.data.PreferencesRepository
import fr.lpoirier.radios.data.StationRepository

/** Conteneur de dépendances minimal (pas besoin de Hilt pour le MVP). */
class RadiosApp : Application() {
    val stations by lazy { StationRepository(this) }
    val preferences by lazy { PreferencesRepository(this) }
}

val Context.app: RadiosApp get() = applicationContext as RadiosApp
