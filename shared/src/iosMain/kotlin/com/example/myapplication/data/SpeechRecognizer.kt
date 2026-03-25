package com.example.myapplication.data

actual class SpeechRecognizer {
    actual fun startListening(locale: String, onResult: (String) -> Unit, onError: () -> Unit) {
        onError()
    }
    actual fun stopListening() {}
    actual fun release() {}
}
