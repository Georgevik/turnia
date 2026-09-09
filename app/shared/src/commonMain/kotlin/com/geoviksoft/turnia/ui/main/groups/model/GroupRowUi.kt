package com.geoviksoft.turnia.ui.main.groups.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.geoviksoft.turnia.core.domain.model.GroupId

@Immutable
data class GroupRowUi(
    val id: GroupId,
    val name: String,
    val color: Color,
    val members: Int,
    /** Only an admin can edit the group and accept the people asking to join it. */
    val isAdmin: Boolean,
    /** They left, or were removed, and only their own leftover shifts are still visible. */
    val isRevoked: Boolean = false,
)
