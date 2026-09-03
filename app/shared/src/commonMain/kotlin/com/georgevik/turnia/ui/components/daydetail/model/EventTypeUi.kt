package com.georgevik.turnia.ui.components.daydetail.model

import androidx.compose.runtime.Immutable
import com.georgevik.turnia.core.domain.model.EventType
import com.georgevik.turnia.ui.components.daydetail.components.EventTypeChipUi

@Immutable
data class EventTypeUi(
    val chipUi: EventTypeChipUi,
    val eventType: EventType,
)
