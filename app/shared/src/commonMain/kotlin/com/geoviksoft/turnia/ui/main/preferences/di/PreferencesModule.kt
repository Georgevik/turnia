package com.geoviksoft.turnia.ui.main.preferences.di

import com.geoviksoft.turnia.ui.main.preferences.PreferencesViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val preferencesModule: Module = module {
    viewModelOf(::PreferencesViewModel)
}
