package com.example.myapplication.data

import platform.AVFAudio.AVSpeechBoundaryImmediate
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance

actual class TtsPlayer {
    private val synthesizer = AVSpeechSynthesizer()

    actual fun speak(text: String, locale: String) {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundaryImmediate)
        val utterance = AVSpeechUtterance(string = text)
        utterance.voice = AVSpeechSynthesisVoice(language = localeToBcp47(locale))
        synthesizer.speakUtterance(utterance)
    }

    private fun localeToBcp47(locale: String): String = when (locale) {
        "de" -> "de-DE"
        "fr" -> "fr-FR"
        "en" -> "en-US"
        else -> locale
    }

    actual fun release() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundaryImmediate)
    }
}
