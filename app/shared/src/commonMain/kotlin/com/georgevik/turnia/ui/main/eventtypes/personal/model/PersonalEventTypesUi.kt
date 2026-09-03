package com.georgevik.turnia.ui.main.eventtypes.personal.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

sealed interface PersonalEventTypesUi {
    data object Loading : PersonalEventTypesUi

    data class Success(
        val types: List<PersonalEventTypeRowUi>,
        val userMessage: PersonalEventTypesMessage? = null,
    ) : PersonalEventTypesUi
}

@Immutable
data class PersonalEventTypeRowUi(
    val typeId: String,
    val name: String,
    val acronym: String?,
    val startTime: String?,
    val endTime: String?,
    val color: Color,
)

enum class PersonalEventTypesMessage { DeleteFailed }
