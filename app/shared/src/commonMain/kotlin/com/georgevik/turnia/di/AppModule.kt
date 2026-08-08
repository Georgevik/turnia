package com.georgevik.turnia.di

import com.georgevik.turnia.GreetingViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Presentation/UI layer dependencies (ViewModels and their collaborators).
 */
val appModule: Module = module {
    viewModelOf(::GreetingViewModel)
}
