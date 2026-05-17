package com.example.myapplication.data

import platform.AVFAudio.AVSpeechBoundaryImmediate
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesizerDelegateProtocol
import platform.AVFAudio.AVSpeechUtterance
import platform.Foundation.NSObject

private class SpeechDelegate(private val onFinish: () -> Unit) : NSObject(), AVSpeechSynthesizerDelegateProtocol {
    override fun speechSynthesizer(synthesizer: AVSpeechSynthesizer, didFinishSpeechUtterance: AVSpeechUtterance) {
        onFinish()
    }
}

actual class TtsPlayer {
    private val synthesizer = AVSpeechSynthesizer()
    private var delegate: SpeechDelegate? = null

    actual fun speak(text: String, locale: String, onDone: (() -> Unit)?) {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundaryImmediate)
        if (onDone != null) {
            val d = SpeechDelegate(onDone)
            delegate = d
            synthesizer.setDelegate(d)
        } else {
            delegate = null
            synthesizer.setDelegate(null)
        }
        val utterance = AVSpeechUtterance(string = text)
        utterance.voice = AVSpeechSynthesisVoice(language = localeToBcp47(locale))
        synthesizer.speakUtterance(utterance)
    }

    actual fun stop() {
        delegate = null
        synthesizer.setDelegate(null)
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundaryImmediate)
    }

    private fun localeToBcp47(locale: String): String = when (locale) {
        "de" -> "de-DE"
        "fr" -> "fr-FR"
        "en" -> "en-US"
        else -> locale
    }

    actual fun release() {
        delegate = null
        synthesizer.setDelegate(null)
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundaryImmediate)
    }
}
