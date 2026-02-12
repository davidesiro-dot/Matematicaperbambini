package com.example.matematicaperbambini

interface AppLogger {
    fun warn(tag: String, message: String)
}

object DefaultLogger : AppLogger {
    override fun warn(tag: String, message: String) {
        println("WARN: [$tag] $message")
    }
}
