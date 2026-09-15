package com.geoviksoft.turnia.ui.main.preferences

import com.geoviksoft.turnia.ui.system.AppLanguage

data class PreferencesUi(
    val notificationsEnabled: Boolean = true,
    val language: AppLanguage = AppLanguage.System,
    /** The user's region lets them change their answer to the ads consent message. */
    val adPrivacyOptionsRequired: Boolean = false,
    val userMessage: PreferencesMessage? = null,
)

enum class PreferencesMessage { SaveFailed }
