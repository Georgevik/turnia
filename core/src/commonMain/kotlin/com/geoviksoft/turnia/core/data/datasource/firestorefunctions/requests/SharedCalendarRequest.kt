package com.geoviksoft.turnia.core.data.datasource.firestorefunctions.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SharedCalendarRequest(
    @SerialName("ownerUid") val ownerUid: String,
    @SerialName("from") val from: String,
    @SerialName("to") val to: String,
)

@Serializable
data class SharedCalendarResponse(
    @SerialName("groupEvents") val groupEvents: List<SharedGroupEventResponse> = emptyList(),
    @SerialName("personalEvents") val personalEvents: List<SharedPersonalEventResponse> = emptyList(),
    @SerialName("personalEventTypes")
    val personalEventTypes: List<SharedPersonalEventTypeResponse> = emptyList(),
    @SerialName("groupEventTypeColors") val groupEventTypeColors: Map<String, String> = emptyMap(),
    @SerialName("groupEventTypes")
    val groupEventTypes: Map<String, List<SharedGroupEventTypeResponse>> = emptyMap(),
    @SerialName("groupNames") val groupNames: Map<String, String> = emptyMap(),
)

@Serializable
data class SharedGroupEventResponse(
    @SerialName("groupId") val groupId: String,
    @SerialName("eventId") val eventId: String,
    @SerialName("groupEventTypeId") val groupEventTypeId: String,
    @SerialName("date") val date: String,
    @SerialName("onSwap") val onSwap: Boolean = false,
    @SerialName("ownerId") val ownerId: String,
    @SerialName("assigneeId") val assigneeId: String,
)

@Serializable
data class SharedPersonalEventResponse(
    @SerialName("eventId") val eventId: String,
    @SerialName("personalEventTypeId") val personalEventTypeId: String? = null,
    @SerialName("date") val date: String,
    @SerialName("notes") val notes: String? = null,
)

@Serializable
data class SharedPersonalEventTypeResponse(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("color") val color: String,
    @SerialName("acronym") val acronym: String = "",
    @SerialName("description") val description: String? = null,
    @SerialName("startTime") val startTime: String? = null,
    @SerialName("endTime") val endTime: String? = null,
    @SerialName("isDeleted") val isDeleted: Boolean = false,
)

@Serializable
data class SharedGroupEventTypeResponse(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("acronym") val acronym: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("startTime") val startTime: String? = null,
    @SerialName("endTime") val endTime: String? = null,
    @SerialName("swappable") val swappable: Boolean = true,
    @SerialName("color") val color: String? = null,
)
