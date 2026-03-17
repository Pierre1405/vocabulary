package com.example.myapplication.data

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.myapplication.db.VocabularyDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        // Copie vocabulary.db depuis les assets si nécessaire
        val dbFile = context.getDatabasePath("vocabulary.db")
        if (!dbFile.exists()) {
            dbFile.parentFile?.mkdirs()
            context.assets.open("vocabulary.db").use { input ->
                dbFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        return AndroidSqliteDriver(
            schema = VocabularyDatabase.Schema,
            context = context,
            name = "vocabulary.db"
        )
    }
}
