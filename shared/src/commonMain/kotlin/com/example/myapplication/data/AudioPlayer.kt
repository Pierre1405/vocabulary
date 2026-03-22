package com.example.myapplication.data

expect class AudioPlayer {
    fun play(phraseId: Long, language: String, onComplete: () -> Unit = {})
    fun release()
}
