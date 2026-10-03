package com.evgenykon.travelguide.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

enum class PlaybackState { IDLE, PLAYING, PAUSED }

class AudioPlayer(context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var player: MediaPlayer? = null

    private val stateState = MutableStateFlow(PlaybackState.IDLE)
    val state: StateFlow<PlaybackState> = stateState.asStateFlow()

    private val labelState = MutableStateFlow<String?>(null)
    val label: StateFlow<String?> = labelState.asStateFlow()

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(audioAttributes)
        .setOnAudioFocusChangeListener { }
        .build()

    fun play(file: File, label: String? = null) {
        stop()
        val mediaPlayer = MediaPlayer()
        player = mediaPlayer
        labelState.value = label
        mediaPlayer.setAudioAttributes(audioAttributes)
        mediaPlayer.setDataSource(file.absolutePath)
        mediaPlayer.setOnCompletionListener { release(it) }
        mediaPlayer.setOnErrorListener { mp, _, _ ->
            release(mp)
            true
        }
        mediaPlayer.prepare()
        audioManager.requestAudioFocus(focusRequest)
        mediaPlayer.start()
        stateState.value = PlaybackState.PLAYING
    }

    fun pause() {
        val current = player ?: return
        if (stateState.value == PlaybackState.PLAYING) {
            runCatching { current.pause() }
            stateState.value = PlaybackState.PAUSED
        }
    }

    fun resume() {
        val current = player ?: return
        if (stateState.value == PlaybackState.PAUSED) {
            runCatching { current.start() }
            stateState.value = PlaybackState.PLAYING
        }
    }

    fun stop() {
        player?.let {
            runCatching { if (it.isPlaying) it.stop() }
            release(it)
        }
    }

    private fun release(mediaPlayer: MediaPlayer) {
        if (player === mediaPlayer) {
            player = null
            stateState.value = PlaybackState.IDLE
            labelState.value = null
        }
        runCatching { mediaPlayer.release() }
        audioManager.abandonAudioFocusRequest(focusRequest)
    }
}
