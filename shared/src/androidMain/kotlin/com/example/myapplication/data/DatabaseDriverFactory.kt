package com.example.myapplication.data

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.myapplication.db.VocabularyDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        val prefs = context.getSharedPreferences("db_prefs", Context.MODE_PRIVATE)
        val storedVersion = prefs.getInt("db_version", 0)
        val dbFile = context.getDatabasePath("vocabulary.db")

        if (!dbFile.exists() || storedVersion != DB_VERSION) {
            dbFile.parentFile?.mkdirs()
            context.assets.open("vocabulary.db").use { input ->
                dbFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            prefs.edit().putInt("db_version", DB_VERSION).apply()
        }

        return AndroidSqliteDriver(
            schema = VocabularyDatabase.Schema,
            context = context,
            name = "vocabulary.db"
        )
    }
}
