package fr.lpoirier.radios.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** État du lecteur exposé à l'UI. */
data class PlayerState(
    val stationId: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    /** Titre ICY en cours (« Artiste - Titre »), si la radio le diffuse. */
    val nowPlaying: String? = null,
    val error: String? = null,
    val volume: Float = 1f,
)

/**
 * Connexion de l'UI au [RadioPlaybackService] via un [MediaController].
 * Les commandes passent par la session : l'UI et la notification restent synchronisées.
 */
class PlayerConnection(context: Context) {

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private val pendingActions = mutableListOf<(MediaController) -> Unit>()

    private val controllerFuture = MediaController.Builder(
        context,
        SessionToken(context, ComponentName(context, RadioPlaybackService::class.java)),
    ).buildAsync()

    init {
        controllerFuture.addListener({
            val c = runCatching { controllerFuture.get() }.getOrNull() ?: return@addListener
            controller = c
            c.addListener(object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) = refresh(player)
            })
            refresh(c)
            pendingActions.forEach { it(c) }
            pendingActions.clear()
        }, ContextCompat.getMainExecutor(context))
    }

    fun play(stationId: String) = withController { c ->
        if (c.currentMediaItem?.mediaId == stationId) {
            c.play()
        } else {
            // Seul le mediaId est utile : le service résout l'URI (onAddMediaItems).
            c.setMediaItem(MediaItem.Builder().setMediaId(stationId).build())
            c.prepare()
            c.play()
        }
    }

    fun pause() = withController { it.pause() }

    fun stop() = withController {
        it.stop()
        it.clearMediaItems()
    }

    fun setVolume(volume: Float) = withController { it.volume = volume.coerceIn(0f, 1f) }

    fun release() {
        MediaController.releaseFuture(controllerFuture)
        controller = null
    }

    private fun withController(action: (MediaController) -> Unit) {
        controller?.let(action) ?: pendingActions.add(action)
    }

    private fun refresh(player: Player) {
        val metadata = player.mediaMetadata
        val title = metadata.title?.toString()
        _state.value = PlayerState(
            stationId = player.currentMediaItem?.mediaId,
            isPlaying = player.isPlaying,
            isBuffering = player.playbackState == Player.STATE_BUFFERING && player.playWhenReady,
            nowPlaying = title?.takeIf { it.isNotBlank() && it != metadata.station?.toString() },
            error = player.playerError?.errorCodeName,
            volume = player.volume,
        )
    }
}
