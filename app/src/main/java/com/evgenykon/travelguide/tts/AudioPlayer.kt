package com.evgenykon.travelguide.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import java.io.File

class AudioPlayer(context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var player: MediaPlayer? = null

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(audioAttributes)
        .setOnAudioFocusChangeListener { }
        .build()

    fun play(file: File) {
        stop()
        val mediaPlayer = MediaPlayer()
        player = mediaPlayer
        mediaPlayer.setAudioAttributes(audioAttributes)
        mediaPlayer.setDataSource(file.absolutePath)
        mediaPlayer.setOnCompletionListener {
            release(it)
        }
        mediaPlayer.setOnErrorListener { mp, _, _ ->
            release(mp)
            true
        }
        mediaPlayer.prepare()
        audioManager.requestAudioFocus(focusRequest)
        mediaPlayer.start()
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
        }
        runCatching { mediaPlayer.release() }
        audioManager.abandonAudioFocusRequest(focusRequest)
    }
}
