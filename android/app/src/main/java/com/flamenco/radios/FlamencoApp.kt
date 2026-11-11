package com.flamenco.radios

import android.app.Application
import android.content.Context
import com.flamenco.radios.data.PreferencesRepository
import com.flamenco.radios.data.StationRepository

/** Conteneur de dépendances minimal (pas besoin de Hilt pour le MVP). */
class FlamencoApp : Application() {
    val stations by lazy { StationRepository(this) }
    val preferences by lazy { PreferencesRepository(this) }
}

val Context.app: FlamencoApp get() = applicationContext as FlamencoApp
