package com.geoviksoft.turnia.core.data.group

import com.geoviksoft.turnia.core.domain.model.EventTypeId
import com.geoviksoft.turnia.core.domain.model.GroupEventType
import com.geoviksoft.turnia.core.domain.model.GroupId
import kotlin.test.Test
import kotlin.test.assertEquals

class PendingEventTypesTest {

    private val pending = PendingEventTypes()

    @Test
    fun anEditKeepsItsPlace() {
        pending.set(type("m"))
        pending.set(type("t"))

        pending.set(type("m", name = "Early"))

        assertEquals(listOf("Early", "t"), pending.types.value.map { it.name })
    }

    @Test
    fun aRemovedTypeLeavesTheOthers() {
        pending.set(type("m"))
        pending.set(type("t"))
        pending.set(type("n"))

        pending.remove(EventTypeId("t"))

        assertEquals(listOf("m", "n"), pending.types.value.map { it.id.value })
    }

    @Test
    fun consumingHandsThemOverOnce() {
        pending.set(type("m"))

        assertEquals(1, pending.consume().size)
        assertEquals(emptyList(), pending.types.value)
    }

    private fun type(id: String, name: String = id) = GroupEventType(
        id = EventTypeId(id),
        groupId = GroupId(""),
        groupName = "",
        name = name,
        acronym = null,
        description = null,
        startTime = null,
        endTime = null,
        swappable = true,
        defaultColor = "#000000",
        userColor = null,
    )
}
