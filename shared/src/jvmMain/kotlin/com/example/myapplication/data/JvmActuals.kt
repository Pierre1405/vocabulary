package com.example.myapplication.data

import app.cash.sqldelight.db.SqlDriver

actual class AudioPlayer {
    actual fun play(sentenceKey: String, language: String, onComplete: () -> Unit) = Unit
    actual fun release() = Unit
}

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver = throw UnsupportedOperationException("JVM stub")
}

actual class DictionaryDriverFactory {
    actual fun createDriver(): SqlDriver = throw UnsupportedOperationException("JVM stub")
}

actual class LearningDriverFactory {
    actual fun createDriver(): SqlDriver = throw UnsupportedOperationException("JVM stub")
}

actual class SpeechRecognizer {
    actual fun startListening(locale: String, onResult: (String) -> Unit, onError: () -> Unit) = Unit
    actual fun stopListening() = Unit
    actual fun release() = Unit
}

actual class TtsPlayer {
    actual fun speak(text: String, locale: String) = Unit
    actual fun release() = Unit
}

actual fun currentEpochHours(): Long = System.currentTimeMillis() / 3_600_000L
