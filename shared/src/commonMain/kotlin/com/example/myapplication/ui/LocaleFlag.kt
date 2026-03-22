package com.example.myapplication.ui

fun localeToFlag(locale: String): String = when (locale) {
    "fr" -> "🇫🇷"
    "de" -> "🇩🇪"
    "en" -> "🇬🇧"
    "es" -> "🇪🇸"
    "it" -> "🇮🇹"
    "pt" -> "🇵🇹"
    "nl" -> "🇳🇱"
    "pl" -> "🇵🇱"
    "ru" -> "🇷🇺"
    "zh" -> "🇨🇳"
    "ja" -> "🇯🇵"
    "ko" -> "🇰🇷"
    else -> "🏳️"
}
