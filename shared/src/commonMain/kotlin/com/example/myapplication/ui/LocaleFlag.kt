package com.example.myapplication.ui

import androidx.compose.ui.graphics.Color

fun gradeColor(grade: Int): Color = when (grade) {
    1 -> Color(0xFFE53935)
    2 -> Color(0xFFFF7043)
    3 -> Color(0xFFFFB300)
    4 -> Color(0xFF7CB342)
    5 -> Color(0xFF43A047)
    else -> Color.Gray
}

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
