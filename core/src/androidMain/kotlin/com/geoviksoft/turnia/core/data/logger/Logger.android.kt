package com.geoviksoft.turnia.core.data.logger

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Logcat, plus Crashlytics.
 *
 * A stack trace on its own rarely says what the app was doing. Every line here becomes a breadcrumb
 * attached to whatever crash comes next, and an error carrying a throwable is also reported as a
 * non-fatal — those are the failures the app handled and the user never told us about.
 *
 * Android only: Crashlytics has no Kotlin/Native binding, so the iOS logger writes to NSLog and
 * reports only the crashes the Swift SDK catches by itself.
 */
actual object Logger {

    private val crashlytics get() = FirebaseCrashlytics.getInstance()

    actual fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    actual fun i(tag: String, message: String) {
        Log.i(tag, message)
        crashlytics.log("I/$tag: $message")
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
        crashlytics.log("E/$tag: $message")
        throwable?.let { crashlytics.recordException(it) }
    }

    actual fun e(tag: String, throwable: Throwable?) {
        Log.e(tag, throwable?.message, throwable)
        crashlytics.log("E/$tag: ${throwable?.message}")
        throwable?.let { crashlytics.recordException(it) }
    }

    actual fun w(tag: String, message: String) {
        Log.w(tag, message)
        crashlytics.log("W/$tag: $message")
    }
}
