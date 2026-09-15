package com.geoviksoft.turnia.core.system

/**
 * What kind of binary is running, handed in by each platform's entry point: a KMP library has no
 * `BuildConfig` of its own to read it from.
 */
data class BuildInfo(val isDebug: Boolean)
