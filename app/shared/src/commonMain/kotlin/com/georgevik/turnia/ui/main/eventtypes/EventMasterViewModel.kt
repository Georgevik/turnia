package com.georgevik.turnia.ui.main.eventtypes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.domain.model.Group
import com.georgevik.turnia.core.domain.model.PersonalEventType
import com.georgevik.turnia.core.domain.repository.GroupRepository
import com.georgevik.turnia.core.domain.repository.PersonalEventRepository
import com.georgevik.turnia.navigation.main.routes.EventTypeKind
import com.georgevik.turnia.ui.main.eventtypes.model.EventTypeMasterHeaderUi
import com.georgevik.turnia.ui.main.eventtypes.model.EventTypeMasterRowUi
import com.georgevik.turnia.ui.main.eventtypes.model.EventTypeMasterUi
import com.georgevik.turnia.ui.system.entityColor
import com.georgevik.turnia.ui.system.toComposeColorOr
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class EventMasterViewModel(
    groupId: String?,
    groupName: String?,
    groupRepository: GroupRepository,
    personalRepository: PersonalEventRepository,
) : ViewModel() {

    private val _query = MutableStateFlow(groupName)
    private val _groupId = MutableStateFlow(groupId)

    val uiState: StateFlow<EventTypeMasterUi> = combine(
        _query,
        _groupId,
        groupRepository.groups.toUiRow(),
        personalRepository.personalEventTypes.map { types -> types.map { it.toUiRow() } },
    ) { query, groupIdQuery, groupTypeRows, personalTypeRows ->
        buildState(query.orEmpty().trim(), groupIdQuery, groupTypeRows, personalTypeRows)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EventTypeMasterUi(query = groupName.orEmpty()),
    )

    fun onQueryChange(query: String) {
        _groupId.update { null }
        _query.update { query }
    }

    private fun buildState(
        query: String,
        groupIdQuery: String?,
        groupTypeRows: List<EventTypeMasterRowUi>,
        allPersonalTypeRows: List<EventTypeMasterRowUi>
    ): EventTypeMasterUi {
        val groupRows: List<EventTypeMasterRowUi>
        val personalTypeRows: List<EventTypeMasterRowUi>

        if (groupIdQuery != null) {
            groupRows = groupTypeRows.filter { it.groupId == groupIdQuery }
            personalTypeRows = emptyList()
        } else if (query.isEmpty()) {
            groupRows = groupTypeRows
            personalTypeRows = allPersonalTypeRows
        } else {
            groupRows = groupTypeRows.filter { it.matches(query) }
            personalTypeRows = allPersonalTypeRows.filter { it.matches(query) }
        }

        val sections = buildList {
            if (personalTypeRows.isNotEmpty()) {
                add(
                    EventTypeMasterHeaderUi(
                        EventTypeKind.PERSONAL, "", personalTypeRows.sortedBy { it.name })
                )
            }
            addAll(groupRows.groupBy { it.groupId }.mapNotNull { (_, groupRows) ->
                val groupName = groupRows.firstOrNull()?.groupName ?: return@mapNotNull null
                EventTypeMasterHeaderUi(
                    kind = EventTypeKind.GROUP,
                    name = groupName,
                    rows = groupRows.sortedBy { it.name })
            }.filter { it.rows.isNotEmpty() }.sortedBy { it.name })
        }

        return EventTypeMasterUi(
            query = query,
            sections = sections,
        )
    }

    private fun EventTypeMasterRowUi.matches(q: String): Boolean =
        name.contains(q, ignoreCase = true) || groupName.orEmpty()
            .contains(q, ignoreCase = true) || acronym?.contains(q, ignoreCase = true) == true

    fun Flow<List<Group>>.toUiRow(): Flow<List<EventTypeMasterRowUi>> = this.map { groups ->
        groups.flatMap { group ->
            group.types.map { type ->
                EventTypeMasterRowUi(
                    kind = EventTypeKind.GROUP,
                    groupId = group.id,
                    typeId = type.id,
                    groupName = group.name,
                    name = type.name,
                    acronym = type.acronym,
                    color = type.color.toComposeColorOr(entityColor(type.id)),
                )
            }
        }
    }

    private fun PersonalEventType.toUiRow() = EventTypeMasterRowUi(
        kind = EventTypeKind.PERSONAL,
        groupId = null,
        typeId = id,
        groupName = null,
        acronym = acronym,
        name = name,
        color = color.toComposeColorOr(entityColor(id)),
    )
}
