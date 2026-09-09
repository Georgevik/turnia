package com.georgevik.turnia.ui.group.detail.di

import com.georgevik.turnia.core.domain.model.GroupId
import com.georgevik.turnia.navigation.main.routes.MainRoute
import com.georgevik.turnia.ui.group.detail.GroupDetailViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Group detail, which is also where a group is created — hence the nullable id. */
val groupDetailModule: Module = module {
    viewModel { (key: MainRoute.GroupDetail) ->
        GroupDetailViewModel(key.groupId?.let(::GroupId), get())
    }
}
