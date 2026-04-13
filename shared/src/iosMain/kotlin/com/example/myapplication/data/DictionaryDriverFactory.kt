package com.example.myapplication.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.example.myapplication.db.dictionary.DictionaryDatabase
import platform.Foundation.NSBundle
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask

actual class DictionaryDriverFactory {
    actual fun createDriver(): SqlDriver {
        val fileManager = NSFileManager.defaultManager
        val docsDir = fileManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null
        )!!.path!!
        val dbPath = "$docsDir/dictionary.db"

        val defaults = NSUserDefaults.standardUserDefaults
        val storedVersion = defaults.integerForKey("dictionary_db_version").toInt()

        if (!fileManager.fileExistsAtPath(dbPath) || storedVersion != DICTIONARY_DB_VERSION) {
            if (fileManager.fileExistsAtPath(dbPath)) {
                fileManager.removeItemAtPath(dbPath, error = null)
            }
            val bundlePath = NSBundle.mainBundle.pathForResource("dictionary", ofType = "db")
            if (bundlePath != null) {
                fileManager.copyItemAtPath(bundlePath, toPath = dbPath, error = null)
            }
            defaults.setInteger(DICTIONARY_DB_VERSION.toLong(), forKey = "dictionary_db_version")
        }

        return NativeSqliteDriver(DictionaryDatabase.Schema, "dictionary.db")
    }
}
