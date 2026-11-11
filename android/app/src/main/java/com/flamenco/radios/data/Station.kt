package com.flamenco.radios.data

import kotlinx.serialization.Serializable

/** Racine de `config/radios.json` (voir `config/radios.schema.json`). */
@Serializable
data class RadioConfig(
    val version: String,
    val generated: String,
    val categories: Map<String, String>,
    val stations: List<Station>,
)

@Serializable
data class Station(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val stream: String,
    val homepage: String,
    val country: String,
    val region: String? = null,
    val codec: String,
    val bitrateKbps: Int,
    val genres: List<String>,
    val language: String? = null,
    /** Data URI `data:image/(png|webp);base64,…`, 128×128. */
    val logo: String,
    val logoSourceUrl: String? = null,
    val notes: String? = null,
) {
    /** Octets bruts du logo, pour l'artwork de la notification média. */
    fun logoBytes(): ByteArray? = runCatching {
        android.util.Base64.decode(logo.substringAfter(","), android.util.Base64.DEFAULT)
    }.getOrNull()

    /** « 128 kbps MP3 · Séville » */
    val subtitle: String
        get() = buildString {
            append("$bitrateKbps kbps $codec")
            region?.let { append(" · ").append(it) }
        }
}
