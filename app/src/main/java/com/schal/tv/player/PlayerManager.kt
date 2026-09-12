package com.schal.tv.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/** État exposé à l'UI, distinct du StreamStatus du catalogue (celui-ci est
 *  l'état de LECTURE en cours, pas l'état de disponibilité déclarée). */
sealed class PlaybackState {
    object Idle : PlaybackState()
    object Buffering : PlaybackState()
    object Playing : PlaybackState()
    object Paused : PlaybackState()
    object Ended : PlaybackState()
    data class Error(val message: String) : PlaybackState()
}

interface PlayerListener {
    fun onStateChanged(state: PlaybackState)
}

/**
 * Encapsule androidx.media3.ExoPlayer. Toute la logique play/pause/reprise/
 * volume/erreurs passe par cette classe pour que l'UI (MainActivity /
 * PlayerActivity) reste simple et testable séparément.
 */
class PlayerManager(context: Context) {

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()
    private var listener: PlayerListener? = null

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val mapped = when (playbackState) {
                    Player.STATE_BUFFERING -> PlaybackState.Buffering
                    Player.STATE_READY -> if (exoPlayer.playWhenReady) PlaybackState.Playing else PlaybackState.Paused
                    Player.STATE_ENDED -> PlaybackState.Ended
                    else -> PlaybackState.Idle
                }
                listener?.onStateChanged(mapped)
            }

            override fun onPlayerError(error: PlaybackException) {
                // Flux indisponible/format non supporté/timeout : jamais de
                // crash, toujours un message clair remonté à l'UI.
                listener?.onStateChanged(PlaybackState.Error(describeError(error)))
            }
        })
    }

    fun setListener(l: PlayerListener?) {
        listener = l
    }

    fun attachView(playerView: androidx.media3.ui.PlayerView) {
        playerView.player = exoPlayer
    }

    /** URL vide/blanche => on refuse de lancer la lecture (pas de flux fabriqué). */
    fun play(streamOrPath: String, resumePositionMs: Long = 0L) {
        if (streamOrPath.isBlank()) {
            listener?.onStateChanged(PlaybackState.Error("Flux non configuré"))
            return
        }
        val mediaItem = MediaItem.fromUri(streamOrPath)
        exoPlayer.setMediaItem(mediaItem, resumePositionMs)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    fun pause() {
        exoPlayer.playWhenReady = false
    }

    fun resume() {
        exoPlayer.playWhenReady = true
    }

    fun currentPositionMs(): Long = exoPlayer.currentPosition

    fun setVolumePercent(percent: Int) {
        exoPlayer.volume = (percent.coerceIn(0, 100)) / 100f
    }

    fun release() {
        exoPlayer.release()
    }

    private fun describeError(error: PlaybackException): String = when (error.errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "INTERNET NÉCESSAIRE : serveur inaccessible"
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "Flux indisponible"
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> "Format non supporté"
        else -> "Lecture impossible (${error.errorCodeName})"
    }
}
