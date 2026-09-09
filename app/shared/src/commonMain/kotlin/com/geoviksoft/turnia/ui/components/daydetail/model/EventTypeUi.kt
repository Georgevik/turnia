package com.geoviksoft.turnia.ui.components.daydetail.model

import androidx.compose.runtime.Immutable
import com.geoviksoft.turnia.core.domain.model.EventType
import com.geoviksoft.turnia.ui.components.daydetail.components.EventTypeChipUi

@Immutable
data class EventTypeUi(
    val chipUi: EventTypeChipUi,
    val eventType: EventType,
)
