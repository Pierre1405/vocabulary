package com.example.myapplication.data

expect class SpeechRecognizer {
    fun startListening(locale: String, onResult: (String) -> Unit, onError: () -> Unit)
    fun stopListening()
    fun release()
}
