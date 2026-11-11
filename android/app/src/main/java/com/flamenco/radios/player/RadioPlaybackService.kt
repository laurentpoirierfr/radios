package com.flamenco.radios.player

import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.flamenco.radios.MainActivity
import com.flamenco.radios.app
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Service de lecture en arrière-plan : ExoPlayer + MediaSession.
 * Media3 fournit la notification média, les contrôles casque/Bluetooth/écran
 * verrouillé et le passage en service de premier plan.
 */
@UnstableApi
class RadioPlaybackService : MediaSessionService() {

    private var session: MediaSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private var retryCount = 0

    override fun onCreate() {
        super.onCreate()

        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(USER_AGENT)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
            // zeno.fm redirige (302) vers des CDN ; certaines redirections changent de protocole.
            .setAllowCrossProtocolRedirects(true)

        val exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(DefaultDataSource.Factory(this, httpFactory)))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true) // pause si le casque est débranché
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        exoPlayer.addListener(ReconnectListener(exoPlayer))

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        session = MediaSession.Builder(this, RadioPlayer(exoPlayer))
            .setSessionActivity(openApp)
            .setCallback(SessionCallback())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        // L'utilisateur ferme l'app depuis les tâches récentes : on s'arrête si rien ne joue.
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    /**
     * Les contrôleurs n'envoient que le `mediaId` (l'URI n'est pas transmise à
     * la session) : on reconstruit l'item complet depuis la configuration.
     */
    private inner class SessionCallback : MediaSession.Callback {
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> {
            val resolved = mediaItems.mapNotNull { item ->
                app.stations.station(item.mediaId)?.toMediaItem()
            }
            return Futures.immediateFuture(resolved)
        }

        /** Reprise de lecture (ex. bouton play d'un casque Bluetooth, app fermée). */
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            isForPlayback: Boolean,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
            scope.launch {
                val station = app.preferences.lastStationIdNow()?.let(app.stations::station)
                    ?: app.stations.stations.first()
                future.set(
                    MediaSession.MediaItemsWithStartPosition(
                        listOf(station.toMediaItem()),
                        /* startIndex = */ 0,
                        /* startPositionMs = */ C.TIME_UNSET,
                    ),
                )
            }
            return future
        }
    }

    /** Reconnexion automatique en cas de coupure réseau (backoff 2 s, 4 s, 8 s…). */
    private inner class ReconnectListener(private val player: ExoPlayer) : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            if (retryCount >= MAX_RETRIES || !player.playWhenReady) return
            val delayMs = 2_000L shl retryCount
            retryCount++
            handler.postDelayed({
                if (player.playWhenReady && player.playbackState == Player.STATE_IDLE) {
                    player.prepare()
                }
            }, delayMs)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) retryCount = 0
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            retryCount = 0
            handler.removeCallbacksAndMessages(null)
        }
    }

    private companion object {
        const val USER_AGENT = "FlamencoRadios/0.1 (Android; Media3)"
        const val MAX_RETRIES = 5
    }
}
