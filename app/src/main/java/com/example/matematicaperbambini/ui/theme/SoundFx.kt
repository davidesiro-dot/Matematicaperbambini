package com.example.matematicaperbambini

import android.content.Context
import android.media.MediaPlayer

class SoundFx(private val context: Context) {

    companion object {
        private var sharedIntroPlayer: MediaPlayer? = null
    }

    private val introPlayer: MediaPlayer?
        get() = sharedIntroPlayer

    fun playIntro() {
        if (sharedIntroPlayer == null) {
            sharedIntroPlayer = MediaPlayer.create(context.applicationContext, R.raw.intro_music).apply {
                isLooping = true
                setVolume(0.5f, 0.5f)
            }
        }

        if (introPlayer?.isPlaying == false) {
            introPlayer?.start()
        }
    }

    fun stopIntro() {
        introPlayer?.pause()
        introPlayer?.seekTo(0)
    }

    fun correct() {}
    fun wrong() {}
    fun bonus() {}

    fun release() {
        // Intentionally kept across configuration changes to avoid restarting music.
    }
}
