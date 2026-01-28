package com.example.matematicaperbambini

import android.content.Context
import android.media.MediaPlayer

class SoundFx(private val context: Context) {

    private var introPlayer: MediaPlayer? = null

    fun playIntro() {
        if (introPlayer == null) {
            introPlayer = MediaPlayer.create(context, R.raw.intro_music).apply {
                isLooping = true
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
        introPlayer?.release()
        introPlayer = null
    }
}
