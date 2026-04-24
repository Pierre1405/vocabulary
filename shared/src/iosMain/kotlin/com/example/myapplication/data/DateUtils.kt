package com.example.myapplication.data

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual fun currentEpochHours(): Long = (NSDate().timeIntervalSince1970 / 3600.0).toLong()
