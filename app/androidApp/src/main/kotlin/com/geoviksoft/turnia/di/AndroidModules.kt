package com.geoviksoft.turnia.di

import com.geoviksoft.turnia.MainActivityViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

 val AndroidAppModule = module {
    viewModelOf(::MainActivityViewModel)
}
