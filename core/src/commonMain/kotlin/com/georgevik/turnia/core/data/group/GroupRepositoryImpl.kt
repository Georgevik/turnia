package com.georgevik.turnia.core.data.group

import com.georgevik.turnia.core.domain.model.GroupEventType
import com.georgevik.turnia.core.domain.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Mock [GroupRepository] used until the Firestore-backed impl exists. It serves a
 * fixed demo set of published event types (same set for any groupId) and keeps the
 * current user's chosen colors in memory. Swap this out without touching callers.
 */
class GroupRepositoryImpl : GroupRepository {

    // Stable ids so the color map keys stay consistent across recompositions/screens.
    private val demoTypes = listOf(
        GroupEventType(
            id = "gt-night",
            name = "Guardia noche",
            acronym = "GN",
            description = "Guardia de 12 horas en turno de noche.",
            startTime = "20:00",
            endTime = "08:00",
            swappable = true,
        ),
        GroupEventType(
            id = "gt-morning",
            name = "Turno mañana",
            acronym = "M",
            description = "Turno de mañana en planta.",
            startTime = "08:00",
            endTime = "15:00",
            swappable = true,
        ),
        GroupEventType(
            id = "gt-afternoon",
            name = "Turno tarde",
            acronym = "T",
            description = "Turno de tarde en planta.",
            startTime = "15:00",
            endTime = "22:00",
            swappable = true,
        ),
        GroupEventType(
            id = "gt-training",
            name = "Formación",
            acronym = "F",
            description = "Sesión de formación interna. No intercambiable.",
            startTime = "16:00",
            endTime = "18:00",
            swappable = false,
        ),
    )

    private val _groupTypeColors = MutableStateFlow<Map<String, String>>(emptyMap())
    override val groupTypeColors: StateFlow<Map<String, String>> = _groupTypeColors.asStateFlow()

    override fun groupEventTypes(groupId: String): List<GroupEventType> = demoTypes

    override fun colorHexFor(groupId: String, typeId: String): String? =
        _groupTypeColors.value[colorKey(groupId, typeId)]

    override fun setGroupTypeColor(groupId: String, typeId: String, hex: String) {
        _groupTypeColors.update { it + (colorKey(groupId, typeId) to hex) }
    }

    // Mock: the demo type set is shared across all groups, so key colors by typeId
    // only. This keeps the color a user picks in the edit screen consistent with what
    // the add-event chips read (which don't carry a specific groupId). The real
    // Firestore-backed impl will use the composite "{groupId}_{typeId}" default.
    override fun colorKey(groupId: String, typeId: String): String = typeId
}
