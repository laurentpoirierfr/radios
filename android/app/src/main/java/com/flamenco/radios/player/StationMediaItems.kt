package com.flamenco.radios.player

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.flamenco.radios.data.Station

/**
 * Construit le [MediaItem] d'une station.
 *
 * Le titre est volontairement laissé vide : ExoPlayer le remplit avec le
 * `StreamTitle` ICY (morceau en cours). [RadioPlayer] se rabat sur le nom de
 * la station tant qu'aucun titre ICY n'est reçu.
 */
fun Station.toMediaItem(genreName: String? = null): MediaItem =
    MediaItem.Builder()
        .setMediaId(id)
        .setUri(stream)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setStation(name)
                .setArtist(name)
                .setGenre(genreName ?: genre)
                .setDescription(description)
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
                .apply {
                    logoBytes()?.let { setArtworkData(it, MediaMetadata.PICTURE_TYPE_FRONT_COVER) }
                }
                .build(),
        )
        .build()
