package com.georgevik.turnia.core.data.logger

import android.util.Log

actual object Logger {
    actual fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    actual fun i(tag: String, message: String) {
        Log.i(tag, message)
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
    }

    actual fun e(tag: String, throwable: Throwable?) {
        Log.e(tag, throwable?.message, throwable)
    }
}
