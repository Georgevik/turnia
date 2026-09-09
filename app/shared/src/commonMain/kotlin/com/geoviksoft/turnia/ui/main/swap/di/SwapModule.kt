package com.geoviksoft.turnia.ui.main.swap.di

import com.geoviksoft.turnia.ui.main.swap.SwapViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val swapModule: Module = module {
    viewModelOf(::SwapViewModel)
}
