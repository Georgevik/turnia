package com.georgevik.turnia.core.domain.repository

import com.georgevik.turnia.core.domain.model.GroupEventType
import kotlinx.coroutines.flow.StateFlow

/**
 * Access to a group's published event types and to the per-user color the current
 * user has chosen for each of them.
 *
 * A [GroupEventType] carries no color (it is shared by every member); instead each
 * user stores their own color in `users/{uid}.groupEventTypeColors`, keyed by the
 * composite `"{groupId}_{typeId}"`. This repo owns that map for the current user.
 */
interface GroupRepository {

    /** Event types the group admin published for [groupId]. */
    fun groupEventTypes(groupId: String): List<GroupEventType>

    /** The current user's chosen colors, keyed by [colorKey]. Hex `#RRGGBB` values. */
    val groupTypeColors: StateFlow<Map<String, String>>

    /** The chosen color hex for this group type, or `null` if the user hasn't picked one. */
    fun colorHexFor(groupId: String, typeId: String): String?

    /** Stores/updates the current user's color for a group type. */
    fun setGroupTypeColor(groupId: String, typeId: String, hex: String)

    /** Composite key used in [groupTypeColors], matching the Firestore map key format. */
    fun colorKey(groupId: String, typeId: String): String = "${groupId}_$typeId"
}
