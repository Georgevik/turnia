package com.geoviksoft.turnia.ui.main.swap.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.UserProfile
import com.geoviksoft.turnia.ui.components.calendar.model.DayEventUi

enum class SwapSegment { OFFERED, COVERED, AVAILABLE }

@Immutable
data class SwapGroupFilterUi(
    val id: GroupId,
    val name: String,
    val color: Color,
    val selected: Boolean,
)


@Immutable
data class SwapOffererUi(
    val name: String,
    val avatar: UserProfile.AnimalAvatar,
)

@Immutable
data class SwapRowUi(
    val event: DayEventUi,
    val offeredBy: SwapOffererUi? = null,
)

sealed interface SwapUi {

    data object Loading : SwapUi

    @Immutable
    data class Success(
        val segments: Map<SwapSegment, List<SwapRowUi>>,
        val segment: SwapSegment,
        val groups: List<SwapGroupFilterUi>,
        val userMessage: SwapMessage? = null,
    ) : SwapUi {
        val rows: List<SwapRowUi> get() = segments[segment].orEmpty()
        val filterable: Boolean get() = groups.size > 1
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
