package com.georgevik.turnia.ui.main.eventtypes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.domain.repository.GroupRepository
import com.georgevik.turnia.core.domain.repository.PersonalEventRepository
import com.georgevik.turnia.navigation.EventTypeKind
import com.georgevik.turnia.ui.main.eventtypes.model.EventTypeMasterHeaderUi
import com.georgevik.turnia.ui.main.eventtypes.model.EventTypeMasterRowUi
import com.georgevik.turnia.ui.main.eventtypes.model.EventTypeMasterUi
import com.georgevik.turnia.ui.system.entityColor
import com.georgevik.turnia.ui.system.toComposeColorOr
import com.georgevik.turnia.ui.system.toComposeColorOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class EventMasterViewModel(
    private val groupId: String,
    private val groupName: String,
    private val groupRepository: GroupRepository,
    personalRepository: PersonalEventRepository,
) : ViewModel() {

    private val _query = MutableStateFlow(groupName)

    val uiState: StateFlow<EventTypeMasterUi> = combine(
        _query,
        groupRepository.groupTypeColors,
        personalRepository.personalEventTypes,
    ) { query, _, personalTypes ->
        buildState(query, personalTypes.map { it.toRow() })
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EventTypeMasterUi(groupName = groupName, query = groupName),
    )

    fun onQueryChange(query: String) {
        _query.value = query
    }

    private fun buildState(query: String, personalRows: List<EventTypeMasterRowUi>): EventTypeMasterUi {
        val q = query.trim()
        val allGroupRows = groupRepository.groupEventTypes(groupId).map { it.toRow() }

        val groupRows: List<EventTypeMasterRowUi>
        val filteredPersonal: List<EventTypeMasterRowUi>
        val groupFirst: Boolean

        if (q.isEmpty()) {
            groupRows = allGroupRows
            filteredPersonal = personalRows
            groupFirst = false
        } else {
            val groupNameMatches = groupName.contains(q, ignoreCase = true)
            groupRows = if (groupNameMatches) allGroupRows else allGroupRows.filter { it.matches(q) }
            filteredPersonal = personalRows.filter { it.matches(q) }
            groupFirst = groupNameMatches
        }

        val personalSection = EventTypeMasterHeaderUi(EventTypeKind.PERSONAL, filteredPersonal)
        val groupSection = EventTypeMasterHeaderUi(EventTypeKind.GROUP, groupRows)
        val ordered = if (groupFirst) listOf(groupSection, personalSection)
        else listOf(personalSection, groupSection)

        return EventTypeMasterUi(
            groupName = groupName,
            query = query,
            sections = ordered.filter { it.rows.isNotEmpty() },
        )
    }

    private fun EventTypeMasterRowUi.matches(q: String): Boolean =
        groupName.contains(q, ignoreCase = true) || acronym?.contains(q, ignoreCase = true) == true

    private fun com.georgevik.turnia.core.domain.model.GroupEventType.toRow() = EventTypeMasterRowUi(
        kind = EventTypeKind.GROUP,
        groupId = groupId,
        typeId = id,
        groupName = name,
        acronym = acronym,
        color = groupRepository.colorHexFor(groupId, id)?.toComposeColorOrNull() ?: entityColor(id),
    )

    private fun com.georgevik.turnia.core.domain.model.PersonalEventType.toRow() = EventTypeMasterRowUi(
        kind = EventTypeKind.PERSONAL,
        groupId = null,
        typeId = id,
        groupName = name,
        acronym = acronym,
        color = color.toComposeColorOr(entityColor(id)),
    )
}
