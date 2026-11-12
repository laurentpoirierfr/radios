package fr.lpoirier.radios.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player

/**
 * Enveloppe le lecteur pour adapter son comportement à la radio en direct :
 * - titre affiché = titre ICY, sinon nom de la station (notification, écran verrouillé) ;
 * - « play » après une erreur (état IDLE) relance la connexion au flux.
 */
@OptIn(UnstableApi::class)
class RadioPlayer(player: Player) : ForwardingPlayer(player) {

    override fun getMediaMetadata(): MediaMetadata {
        val metadata = super.getMediaMetadata()
        return if (metadata.title.isNullOrBlank()) {
            metadata.buildUpon().setTitle(metadata.station).build()
        } else {
            metadata
        }
    }

    override fun play() {
        if (playbackState == Player.STATE_IDLE) prepare()
        super.play()
    }
}
