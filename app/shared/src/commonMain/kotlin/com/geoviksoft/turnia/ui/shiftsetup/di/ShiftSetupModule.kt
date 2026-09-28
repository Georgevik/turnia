package com.geoviksoft.turnia.ui.shiftsetup.di

import com.geoviksoft.turnia.core.domain.model.ShiftSetupVia
import com.geoviksoft.turnia.ui.shiftsetup.ShiftSetupViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val shiftSetupModule: Module = module {
    viewModel { (via: ShiftSetupVia) -> ShiftSetupViewModel(via, get()) }
}
