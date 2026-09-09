package com.geoviksoft.turnia.core.domain.model

/**
 * @param defaultColor fixed when the type was created, the same for everyone and never written
 *   again: whoever changes it later, an admin included, changes only their own [userColor]. It is
 *   the background of a shift of this type until the user picks theirs. Empty only in types older
 *   than the field.
 * @param userColor the one this user picked for themselves, which wins over the default.
 */
data class GroupEventType(
    override val id: EventTypeId,
    val groupId: GroupId,
    val groupName: String,
    override val name: String,
    override val acronym: String?,
    override val description: String?,
    override val startTime: String?,
    override val endTime: String?,
    val swappable: Boolean,
    val defaultColor: String,
    val userColor: String?,
    override val isDeleted: Boolean = false
) : EventType {
    override val color = userColor?.takeIf { it.isNotBlank() } ?: defaultColor
}
