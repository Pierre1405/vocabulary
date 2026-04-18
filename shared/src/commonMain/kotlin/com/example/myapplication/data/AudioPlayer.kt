package com.example.myapplication.data

expect class AudioPlayer {
    fun play(sentenceKey: String, language: String, onComplete: () -> Unit = {})
    fun release()
}
