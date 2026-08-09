package com.georgevik.turnia.core.data.logger

expect object Logger {
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, throwable: Throwable?)
}
