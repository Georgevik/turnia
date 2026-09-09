package com.georgevik.turnia.ui.main.settings.di

import com.georgevik.turnia.ui.main.profile.MyProfileViewModel
import com.georgevik.turnia.ui.main.settings.SettingsMenuViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** The settings menu and the profile it opens. */
val settingsModule: Module = module {
    viewModelOf(::SettingsMenuViewModel)
    viewModelOf(::MyProfileViewModel)
}
