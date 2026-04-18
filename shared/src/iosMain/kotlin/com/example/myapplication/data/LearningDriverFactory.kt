package com.example.myapplication.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.example.myapplication.db.learning.LearningDatabase

actual class LearningDriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(LearningDatabase.Schema, "learning.db")
}
