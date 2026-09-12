package com.geoviksoft.turnia.ui.system

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.core.os.LocaleListCompat

actual fun currentAppLanguage(): AppLanguage {
    val locales = AppCompatDelegate.getApplicationLocales()
    val language = if (locales.isEmpty) null else locales[0]?.language
    return AppLanguage.entries.firstOrNull { it.tag != null && it.tag == language } ?: AppLanguage.System
}

actual fun applyAppLanguage(language: AppLanguage) {
    AppCompatDelegate.setApplicationLocales(
        language.tag?.let(LocaleListCompat::forLanguageTags) ?: LocaleListCompat.getEmptyLocaleList()
    )
}

@Composable
actual fun AppLanguageHost(content: @Composable () -> Unit) = content()
