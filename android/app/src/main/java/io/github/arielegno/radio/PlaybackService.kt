package io.github.arielegno.radio

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/** El reproductor vive todo el tiempo que viva la app (lo comparten la pantalla y la notificación). */
object Reproductor {
    private var p: ExoPlayer? = null
    fun get(): ExoPlayer = p ?: ExoPlayer.Builder(RadioApp.app)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
        .setHandleAudioBecomingNoisy(true)
        .build().also { p = it }
}

/** Servicio de reproducción en segundo plano con controles en la notificación y la pantalla bloqueada. */
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val abrir = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        session = MediaSession.Builder(this, Reproductor.get()).setSessionActivity(abrir).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val pl = session?.player
        if (pl == null || !pl.playWhenReady || pl.mediaItemCount == 0 || pl.playbackState == Player.STATE_ENDED) stopSelf()
    }

    override fun onDestroy() {
        session?.release()
        session = null
        super.onDestroy()
    }
}
