package com.example.myapplication.ui

data class SentenceWithTranslations(
    val sentenceKey: String,
    val translations: Map<String, String>  // locale -> texte (ex: "fr" -> "Bonjour")
) {
    fun getTranslation(locale: String): String = translations[locale] ?: ""
}
