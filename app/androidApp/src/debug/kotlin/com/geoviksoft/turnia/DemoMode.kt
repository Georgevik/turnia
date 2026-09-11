package com.geoviksoft.turnia

import android.content.Context
import java.io.File

/**
 * Whether to run on made-up data (store screenshots, demos). Switched by a file in the app's own
 * storage, which only a debuggable build lets `adb` touch:
 *
 * ```
 * adb shell run-as com.geoviksoft.turnia touch files/demo   # on
 * adb shell run-as com.geoviksoft.turnia rm files/demo      # off
 * ```
 *
 * Read once, when the app process starts: force-stop it after flipping the switch.
 */
internal fun isDemoMode(context: Context): Boolean = File(context.filesDir, "demo").exists()
