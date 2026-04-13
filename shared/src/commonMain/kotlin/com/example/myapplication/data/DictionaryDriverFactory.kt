package com.example.myapplication.data

import app.cash.sqldelight.db.SqlDriver

expect class DictionaryDriverFactory {
    fun createDriver(): SqlDriver
}
