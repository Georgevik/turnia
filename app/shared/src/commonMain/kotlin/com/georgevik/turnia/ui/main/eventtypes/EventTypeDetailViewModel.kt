package com.georgevik.turnia.ui.main.eventtypes

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.georgevik.turnia.core.domain.model.PersonalEventType
import com.georgevik.turnia.core.domain.repository.GroupRepository
import com.georgevik.turnia.core.domain.repository.PersonalEventRepository
import com.georgevik.turnia.navigation.EventTypeKind
import com.georgevik.turnia.ui.main.eventtypes.model.EventTypeDetailUi
import com.georgevik.turnia.ui.system.EntityPalette
import com.georgevik.turnia.ui.system.createUuid
import com.georgevik.turnia.ui.system.entityColor
import com.georgevik.turnia.ui.system.toComposeColorOr
import com.georgevik.turnia.ui.system.toComposeColorOrNull
import com.georgevik.turnia.ui.system.toHex
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class EventTypeDetailViewModel(
    private val kind: EventTypeKind,
    private val groupId: String?,
    private val typeId: String?,
    private val groupRepository: GroupRepository,
    private val personalRepository: PersonalEventRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<EventTypeDetailUi> = _uiState.asStateFlow()

    private fun initialState(): EventTypeDetailUi = when (kind) {
        EventTypeKind.GROUP -> {
            val gid = requireNotNull(groupId) { "Group type detail needs a groupId" }
            val tid = requireNotNull(typeId) { "Group type detail needs a typeId" }
            val type = groupRepository.groupEventTypes(gid).first { it.id == tid }
            EventTypeDetailUi(
                kind = kind,
                isCreate = false,
                fieldsEditable = false,
                name = type.name,
                acronym = type.acronym.orEmpty(),
                description = type.description.orEmpty(),
                startTime = type.startTime.orEmpty(),
                endTime = type.endTime.orEmpty(),
                color = groupRepository.colorHexFor(gid, tid)?.toComposeColorOrNull()
                    ?: entityColor(tid),
                swappable = type.swappable,
            )
        }

        EventTypeKind.PERSONAL -> {
            val existing = typeId?.let { personalRepository.byId(it) }
            EventTypeDetailUi(
                kind = kind,
                isCreate = existing == null,
                fieldsEditable = true,
                name = existing?.name.orEmpty(),
                acronym = existing?.acronym.orEmpty(),
                description = existing?.description.orEmpty(),
                startTime = existing?.startTime.orEmpty(),
                endTime = existing?.endTime.orEmpty(),
                color = existing?.color?.toComposeColorOr(entityColor(existing.id))
                    ?: EntityPalette.first(),
                swappable = null,
            )
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }
    fun onAcronymChange(value: String) = _uiState.update { it.copy(acronym = value) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }
    fun onStartTimeChange(value: String) = _uiState.update { it.copy(startTime = value) }
    fun onEndTimeChange(value: String) = _uiState.update { it.copy(endTime = value) }

    fun onPickColor(color: Color) {
        _uiState.update { it.copy(color = color) }
        // Group color is the user's own preference — persist it right away.
        if (kind == EventTypeKind.GROUP && groupId != null && typeId != null) {
            groupRepository.setGroupTypeColor(groupId, typeId, color.toHex())
        }
    }

    /** Personal types only: persists the edited/new template. Returns true if saved. */
    fun onSave(): Boolean {
        if (kind != EventTypeKind.PERSONAL) return false
        val state = _uiState.value
        if (state.name.isBlank()) return false
        val type = PersonalEventType(
            id = typeId ?: createUuid(),
            name = state.name.trim(),
            color = state.color.toHex(),
            acronym = state.acronym.trim().ifBlank { null },
            description = state.description.trim().ifBlank { null },
            startTime = state.startTime.trim().ifBlank { null },
            endTime = state.endTime.trim().ifBlank { null },
        )
        if (state.isCreate) personalRepository.create(type) else personalRepository.update(type)
        return true
    }
}
