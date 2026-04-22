package com.example.myapplication.data

import platform.Foundation.NSDate

actual fun currentEpochDays(): Long = (NSDate().timeIntervalSince1970 / 86400.0).toLong()
