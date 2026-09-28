package com.geoviksoft.turnia.core.data.group

import com.geoviksoft.turnia.core.domain.model.EventTypeId
import com.geoviksoft.turnia.core.domain.model.GroupEventType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update

/** The types held for a group that does not exist yet, in the order they were added. */
class PendingEventTypes {

    private val _types = MutableStateFlow<List<GroupEventType>>(emptyList())
    val types: StateFlow<List<GroupEventType>> = _types.asStateFlow()

    /** Adds [type], or replaces the one it is an edit of where it stands. */
    fun set(type: GroupEventType) = _types.update { held ->
        if (held.none { it.id == type.id }) held + type
        else held.map { if (it.id == type.id) type else it }
    }

    fun remove(id: EventTypeId) = _types.update { held -> held.filterNot { it.id == id } }

    fun consume(): List<GroupEventType> = _types.getAndUpdate { emptyList() }
}
