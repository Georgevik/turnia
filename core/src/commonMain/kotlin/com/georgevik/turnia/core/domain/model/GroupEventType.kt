package com.georgevik.turnia.core.domain.model

/**
 * @param officialColor el que eligió el administrador del grupo, igual para todos. Es el fondo por
 *   defecto de un turno de este tipo. Vacío sólo en los tipos anteriores a que existiera.
 * @param userColor el que este usuario ha elegido para sí, que manda sobre el oficial.
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
    val officialColor: String,
    val userColor: String?,
    override val isDeleted: Boolean = false
) : EventType {
    override val color = userColor?.takeIf { it.isNotBlank() } ?: officialColor
}
