package com.example.myapplication.ui

data class SentenceWithTranslations(
    val sentenceId: Long,
    val translations: Map<String, String>  // locale -> texte (ex: "fr" -> "Bonjour")
) {
    fun getTranslation(locale: String): String = translations[locale] ?: ""
}
