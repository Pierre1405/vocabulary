package com.example.myapplication.data

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.myapplication.db.dictionary.DictionaryDatabase

// Incrémenter à chaque fois que dictionary.db est regénéré
const val DICTIONARY_DB_VERSION = 1

actual class DictionaryDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        val prefs = context.getSharedPreferences("db_prefs", Context.MODE_PRIVATE)
        val storedVersion = prefs.getInt("dictionary_db_version", 0)
        val dbFile = context.getDatabasePath("dictionary.db")

        if (!dbFile.exists() || storedVersion != DICTIONARY_DB_VERSION) {
            dbFile.parentFile?.mkdirs()
            context.assets.open("dictionary.db").use { input ->
                dbFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            prefs.edit().putInt("dictionary_db_version", DICTIONARY_DB_VERSION).apply()
        }

        return AndroidSqliteDriver(
            schema = DictionaryDatabase.Schema,
            context = context,
            name = "dictionary.db"
        )
    }
}
