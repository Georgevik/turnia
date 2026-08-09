package com.georgevik.turnia.core.data.logger

import platform.Foundation.NSLog

actual object Logger {
    actual fun d(tag: String, message: String) {
        NSLog("[$tag] $message")
    }

    actual fun i(tag: String, message: String) {
        NSLog("[$tag] INFO: $message")
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        NSLog("[$tag] ERROR: $message. ${throwable?.message ?: ""}")
    }

    actual fun e(tag: String, throwable: Throwable?) {
        NSLog("[$tag] ERROR: ${throwable?.message ?: ""}")
    }
}
