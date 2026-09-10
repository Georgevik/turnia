package com.geoviksoft.turnia.ui.main.swap.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.ui.components.calendar.model.DayEventUi

/**
 * Which of the four ways a shift can relate to the user is on screen.
 *
 * The order is the order of the tabs, and it goes from what the user is doing to what is being done
 * for them: what I have put up, what I could pick up, what somebody took off me, what I picked up.
 */
enum class SwapSegment { OFFERED, AVAILABLE, COVERED, COVERING }

@Immutable
data class SwapGroupFilterUi(
    val id: GroupId,
    val name: String,
    val color: Color,
    val selected: Boolean,
)

/**
 * No `Error` variant: there is nothing here that can fail terminally. Every segment may legitimately
 * be empty — most of them are, most of the time — so emptiness is the ordinary case and gets a
 * sentence, not an error screen. A load that fails arrives as a [SwapMessage] instead.
 */
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

        /** The row of chips only earns its place once there is more than one group to choose between. */
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
