package com.geoviksoft.turnia.ui.main.swap.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.ui.components.calendar.model.DayEventUi

enum class SwapSegment { OFFERED, COVERED }

@Immutable
data class SwapGroupFilterUi(
    val id: GroupId,
    val name: String,
    val color: Color,
    val selected: Boolean,
)

sealed interface SwapUi {

    data object Loading : SwapUi

    @Immutable
    data class Success(
        val segments: Map<SwapSegment, List<DayEventUi>>,
        val segment: SwapSegment,
        val groups: List<SwapGroupFilterUi>,
        val userMessage: SwapMessage? = null,
    ) : SwapUi {
        val events: List<DayEventUi> get() = segments[segment].orEmpty()
        val filterable: Boolean get() = groups.size > 1
        val allGroupsSelected: Boolean get() = groups.all { it.selected }
    }
}

enum class SwapMessage {
    NotSwappable,
    NotMember,
    OwnShift,
    NotFound,
    TakenBySomeoneElse,
    SaveFailed,
}
