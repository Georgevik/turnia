package com.geoviksoft.turnia.ui.system

import androidx.compose.runtime.Composable

/** The languages the app is translated into, plus following the device. Keep in step with `composeResources`. */
enum class AppLanguage(val tag: String?) {
    System(null),
    English("en"),
    Spanish("es"),
}

/**
 * The language the app is shown in, kept where the operating system keeps it: Android's per-app
 * language (the one under *Settings › Apps › Language*) and iOS's `AppleLanguages`. Not in our own
 * storage, because the system reads it too — it is what renders a push, drawn with the app closed,
 * in the same language as the app.
 */
expect fun currentAppLanguage(): AppLanguage

expect fun applyAppLanguage(language: AppLanguage)

/**
 * Redraws [content] in a newly applied language. Android needs nothing — the system recreates the
 * activity — but iOS only reads the language while composing, so everything below is recomposed.
 */
@Composable
expect fun AppLanguageHost(content: @Composable () -> Unit)
