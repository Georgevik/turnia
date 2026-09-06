package com.georgevik.turnia.ui.main.groups.model

import androidx.compose.runtime.Immutable
import com.georgevik.turnia.core.domain.model.GroupId

@Immutable
data class JoinRequestRowUi(
    val groupId: GroupId,
    val groupName: String,
    val isPending: Boolean,
)
