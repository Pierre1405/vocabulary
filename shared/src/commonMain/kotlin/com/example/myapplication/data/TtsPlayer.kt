package com.example.myapplication.data

expect class TtsPlayer {
    fun speak(text: String, locale: String, onDone: (() -> Unit)? = null)
    fun stop()
    fun release()
}
