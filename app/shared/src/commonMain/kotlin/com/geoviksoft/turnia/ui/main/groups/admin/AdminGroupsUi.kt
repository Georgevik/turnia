package com.geoviksoft.turnia.ui.main.groups.admin

import com.geoviksoft.turnia.ui.main.groups.model.GroupRowUi

sealed interface AdminGroupsUi {

    data object Loading : AdminGroupsUi

    data class Success(val groups: List<GroupRowUi> = emptyList()) : AdminGroupsUi
}
