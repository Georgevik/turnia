package com.geoviksoft.turnia

import android.content.Context

/** A release build always runs on real data. */
@Suppress("UNUSED_PARAMETER")
internal fun isDemoMode(context: Context): Boolean = false
