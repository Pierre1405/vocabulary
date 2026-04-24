package com.example.myapplication.data

actual fun currentEpochHours(): Long = System.currentTimeMillis() / 3_600_000L
