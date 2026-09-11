package com.geoviksoft.turnia.ui.main.groups.di

import com.geoviksoft.turnia.ui.main.groups.GroupsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val groupsModule: Module = module {
    viewModelOf(::GroupsViewModel)
}
