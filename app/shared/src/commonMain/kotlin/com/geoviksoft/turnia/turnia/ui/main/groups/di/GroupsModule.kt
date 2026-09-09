package com.geoviksoft.turnia.ui.main.groups.di

import com.geoviksoft.turnia.ui.main.groups.GroupsViewModel
import com.geoviksoft.turnia.ui.main.groups.admin.AdminGroupsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** The lists of groups: the ones the user belongs to, and the ones they administer. */
val groupsModule: Module = module {
    viewModelOf(::GroupsViewModel)
    viewModelOf(::AdminGroupsViewModel)
}
