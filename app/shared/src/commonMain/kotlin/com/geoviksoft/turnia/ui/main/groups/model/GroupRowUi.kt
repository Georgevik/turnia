package com.geoviksoft.turnia.ui.main.groups.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.geoviksoft.turnia.core.domain.model.Group
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.ui.system.color.entityColor
import com.geoviksoft.turnia.ui.system.color.toComposeColorOrNull

@Immutable
data class GroupRowUi(
    val id: GroupId,
    val name: String,
    val color: Color,
    val members: Int,
    /** Only an admin can edit the group and accept the people asking to join it. */
    val isAdmin: Boolean,
)

fun Group.toRowUi() = GroupRowUi(
    id = id,
    name = name,
    color = color?.toComposeColorOrNull() ?: entityColor(id.value),
    members = memberCount,
    isAdmin = isAdmin,
)
