package com.example.myapplication.data

actual fun currentEpochDays(): Long = System.currentTimeMillis() / 86_400_000L
