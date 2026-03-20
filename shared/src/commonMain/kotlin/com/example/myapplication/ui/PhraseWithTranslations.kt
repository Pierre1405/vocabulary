package com.example.myapplication.ui

data class PhraseWithTranslations(
    val phraseId: Long,
    val translations: Map<String, String>  // locale -> texte (ex: "fr" -> "Bonjour")
) {
    fun getTranslation(locale: String): String = translations[locale] ?: ""
}
