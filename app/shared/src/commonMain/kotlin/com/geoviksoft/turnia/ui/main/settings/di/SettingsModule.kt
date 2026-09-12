package com.geoviksoft.turnia.ui.main.settings.di

import com.geoviksoft.turnia.ui.main.about.AboutViewModel
import com.geoviksoft.turnia.ui.main.profile.MyProfileViewModel
import com.geoviksoft.turnia.ui.main.settings.SettingsMenuViewModel
import com.geoviksoft.turnia.ui.main.settings.mygroups.MyGroupsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** The settings menu and the screens it opens that have no module of their own. */
val settingsModule: Module = module {
    viewModelOf(::SettingsMenuViewModel)
    viewModelOf(::MyProfileViewModel)
    viewModelOf(::AboutViewModel)
    viewModelOf(::MyGroupsViewModel)
}
