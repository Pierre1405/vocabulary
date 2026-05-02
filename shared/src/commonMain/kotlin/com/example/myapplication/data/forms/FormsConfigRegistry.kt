package com.example.myapplication.data.forms

fun getFormsConfig(locale: String): FormsConfig = when (locale) {
    "fr" -> FormsConfigFr
    else -> FormsConfigDe
}
