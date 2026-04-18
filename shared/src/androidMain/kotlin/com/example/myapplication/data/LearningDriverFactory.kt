package com.example.myapplication.data

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.myapplication.db.learning.LearningDatabase

actual class LearningDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(
            schema = LearningDatabase.Schema,
            context = context,
            name = "learning.db"
        )
}
