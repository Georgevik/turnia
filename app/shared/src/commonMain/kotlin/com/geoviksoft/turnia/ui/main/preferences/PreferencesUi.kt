package com.geoviksoft.turnia.ui.main.preferences

import com.geoviksoft.turnia.ui.system.AppLanguage

data class PreferencesUi(
    val notificationsEnabled: Boolean = true,
    val language: AppLanguage = AppLanguage.System,
    val userMessage: PreferencesMessage? = null,
)

enum class PreferencesMessage { SaveFailed }
