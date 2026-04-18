package com.example.myapplication.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.myapplication.db.learning.LearningDatabase

actual class LearningDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(
            schema = LearningDatabase.Schema,
            context = context,
            name = "learning.db",
            callback = object : AndroidSqliteDriver.Callback(LearningDatabase.Schema) {
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS word_learning (
                            entry_id       INTEGER NOT NULL,
                            translation_id INTEGER NOT NULL,
                            grade          INTEGER NOT NULL,
                            PRIMARY KEY (entry_id, translation_id)
                        )
                    """)
                }
            }
        )
}
