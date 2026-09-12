package com.geoviksoft.turnia.ui.system

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import platform.Foundation.NSBundle
import platform.Foundation.NSUserDefaults

private const val APPLE_LANGUAGES = "AppleLanguages"

private var appliedLanguage by mutableStateOf(storedLanguage())

actual fun currentAppLanguage(): AppLanguage = appliedLanguage

actual fun applyAppLanguage(language: AppLanguage) {
    val defaults = NSUserDefaults.standardUserDefaults
    if (language.tag == null) {
        defaults.removeObjectForKey(APPLE_LANGUAGES)
    } else {
        defaults.setObject(listOf(language.tag), forKey = APPLE_LANGUAGES)
    }
    appliedLanguage = language
}

@Composable
actual fun AppLanguageHost(content: @Composable () -> Unit) {
    key(appliedLanguage) { content() }
}

/**
 * Only the app's own domain: `standardUserDefaults` would also answer from the global one, where
 * `AppleLanguages` is the device's list, and an app following the device would read as a choice.
 * iOS's own per-app language setting writes the same key here, so a pick made there shows too.
 */
private fun storedLanguage(): AppLanguage {
    val bundleId = NSBundle.mainBundle.bundleIdentifier ?: return AppLanguage.System
    val domain = NSUserDefaults.standardUserDefaults.persistentDomainForName(bundleId)
    val tag = (domain?.get(APPLE_LANGUAGES) as? List<*>)?.firstOrNull() as? String
        ?: return AppLanguage.System
    return AppLanguage.entries.firstOrNull { it.tag != null && tag.startsWith(it.tag) } ?: AppLanguage.System
}
