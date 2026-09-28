package com.geoviksoft.turnia.ui.components.movetogroup

import com.geoviksoft.turnia.core.domain.model.Group
import com.geoviksoft.turnia.core.domain.model.GroupEventType

/** A revoked member can create nothing in the group, and a group with no types has nothing to move into. */
val Group.canReceiveMovedEvents: Boolean get() = !isRevoked && moveTargetTypes.isNotEmpty()

val Group.moveTargetTypes: List<GroupEventType> get() = types.filterNot { it.isDeleted }
