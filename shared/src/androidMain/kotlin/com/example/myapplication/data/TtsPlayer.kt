package com.example.myapplication.data

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

actual class TtsPlayer(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var initialized = false
    private var pendingText: String? = null
    private var pendingLocale: String? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                initialized = true
                val text = pendingText
                val locale = pendingLocale
                if (text != null && locale != null) speakNow(text, locale)
                pendingText = null
                pendingLocale = null
            }
        }
    }

    actual fun speak(text: String, locale: String) {
        if (initialized) {
            speakNow(text, locale)
        } else {
            pendingText = text
            pendingLocale = locale
        }
    }

    private fun speakNow(text: String, locale: String) {
        val javaLocale = when (locale) {
            "de" -> Locale.GERMAN
            "fr" -> Locale.FRENCH
            "en" -> Locale.ENGLISH
            else -> Locale(locale)
        }
        tts?.setLanguage(javaLocale)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    actual fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
