package com.example.myapplication.data

import android.content.Context
import android.media.MediaPlayer

actual class AudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    actual fun play(phraseId: Long, language: String, onComplete: () -> Unit) {
        mediaPlayer?.release()
        val resourceName = "phrase_${phraseId}_$language"
        val resourceId = context.resources.getIdentifier(resourceName, "raw", context.packageName)
        if (resourceId != 0) {
            mediaPlayer = MediaPlayer.create(context, resourceId)
            mediaPlayer?.setOnCompletionListener { onComplete() }
            mediaPlayer?.start()
        } else {
            onComplete()
        }
    }

    actual fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
