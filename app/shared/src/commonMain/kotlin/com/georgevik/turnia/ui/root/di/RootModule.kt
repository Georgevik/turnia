package com.georgevik.turnia.ui.root.di

import com.georgevik.turnia.navigation.ScreenReporter
import com.georgevik.turnia.ui.main.MainViewModel
import com.georgevik.turnia.ui.root.RootViewModel
import com.georgevik.turnia.ui.signin.SignInViewModel
import com.georgevik.turnia.ui.splash.SplashViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** The shell every other feature is shown inside: splash, sign-in, and the tabbed main screen. */
val rootModule: Module = module {
    single { ScreenReporter(get()) }
    viewModelOf(::RootViewModel)
    viewModelOf(::SplashViewModel)
    viewModelOf(::SignInViewModel)
    viewModelOf(::MainViewModel)
}
