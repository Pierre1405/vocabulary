package com.example.myapplication.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.example.myapplication.db.VocabularyDatabase
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val fileManager = NSFileManager.defaultManager
        val docsDir = fileManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null
        )!!.path!!
        val dbPath = "$docsDir/vocabulary.db"

        // Copie vocabulary.db depuis le bundle iOS si nécessaire
        if (!fileManager.fileExistsAtPath(dbPath)) {
            val bundlePath = NSBundle.mainBundle.pathForResource("vocabulary", ofType = "db")
            if (bundlePath != null) {
                fileManager.copyItemAtPath(bundlePath, toPath = dbPath, error = null)
            }
        }

        return NativeSqliteDriver(VocabularyDatabase.Schema, "vocabulary.db")
    }
}
