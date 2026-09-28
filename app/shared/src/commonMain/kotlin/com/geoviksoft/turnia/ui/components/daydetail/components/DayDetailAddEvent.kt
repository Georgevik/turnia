package com.geoviksoft.turnia.ui.components.daydetail.components

import turnia.app.shared.generated.resources.add_pane_other_event
import androidx.compose.foundation.layout.Box
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.geoviksoft.turnia.core.domain.model.EventType
import com.geoviksoft.turnia.core.domain.model.EventTypeId
import com.geoviksoft.turnia.core.domain.model.GroupEventType
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.PersonalEventType
import com.geoviksoft.turnia.ui.components.daydetail.DayAddMode
import com.geoviksoft.turnia.ui.components.daydetail.model.EventTypeSectionUi
import com.geoviksoft.turnia.ui.components.daydetail.model.EventTypeUi
import com.geoviksoft.turnia.ui.components.daydetail.model.OneOffEventFormUi
import com.geoviksoft.turnia.ui.components.daydetail.model.OneOffFormAction
import com.geoviksoft.turnia.ui.system.PreviewTurniaTheme
import com.geoviksoft.turnia.ui.system.TestTags
import com.geoviksoft.turnia.ui.system.color.EntityPalette
import com.geoviksoft.turnia.ui.system.color.toComposeColorOr
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.day_detail_group_events
import turnia.app.shared.generated.resources.day_detail_personal_events
import turnia.app.shared.generated.resources.day_detail_personal_one_off_events
import turnia.app.shared.generated.resources.event_group_no_types
import turnia.app.shared.generated.resources.event_group_no_types_action
import turnia.app.shared.generated.resources.event_group_only_banner

/** The one-off field and the form it becomes are one element as far as the transition goes. */
private const val ONE_OFF_BOUNDS_KEY = "oneOffEvent"
private const val ONE_OFF_LABEL_KEY = "oneOffEventLabel"
private const val ONE_OFF_BOUNDS_MS = 320

/**
 * The add pane. Writing a one-off event takes it over: the field grows into the form while the
 * templates slide away below it, since nothing else on the pane applies while the form is open.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DayDetailAddEvent(
    addMode: DayAddMode,
    sections: List<EventTypeSectionUi>,
    oneOffForm: OneOffEventFormUi?,
    onOneOffAction: (OneOffFormAction) -> Unit,
    onPickEventType: (eventType: EventTypeUi) -> Unit,
    onEditGroup: (groupId: String, groupName: String) -> Unit,
    onAddPersonalEventType: () -> Unit,
    onAddGroupEventType: (groupId: String) -> Unit,
    modifier: Modifier = Modifier,
    onCreateShifts: () -> Unit = {},
) {
    SharedTransitionLayout(modifier = modifier) {
        AnimatedContent(
            targetState = oneOffForm,
            // Typing changes the form but not what is on screen: only opening and closing animate.
            contentKey = { it != null },
            transitionSpec = {
                if (targetState != null) {
                    fadeIn(tween(durationMillis = 200, delayMillis = 120)) togetherWith
                        (slideOutVertically(tween(ONE_OFF_BOUNDS_MS)) { it / 2 } + fadeOut(tween(200)))
                } else {
                    (slideInVertically(tween(ONE_OFF_BOUNDS_MS)) { it / 2 } + fadeIn(tween(250))) togetherWith
                        fadeOut(tween(150))
                } using SizeTransform(clip = false)
            },
            label = "oneOffForm",
        ) { form ->
            val boundsTransform = BoundsTransform { _, _ ->
                tween(ONE_OFF_BOUNDS_MS, easing = FastOutSlowInEasing)
            }
            val label: @Composable () -> Unit = {
                DayCategoryLabel(
                    text = stringResource(Res.string.day_detail_personal_one_off_events),
                    modifier = Modifier.sharedElement(
                        rememberSharedContentState(ONE_OFF_LABEL_KEY),
                        animatedVisibilityScope = this@AnimatedContent,
                        boundsTransform = boundsTransform,
                    ),
                )
            }
            val bounds = Modifier.sharedBounds(
                rememberSharedContentState(ONE_OFF_BOUNDS_KEY),
                animatedVisibilityScope = this@AnimatedContent,
                boundsTransform = boundsTransform,
            )

            if (form != null) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    label()
                    OneOffEventForm(form = form, onAction = onOneOffAction, modifier = bounds)
                }
            } else {
                EventTypePane(
                    addMode = addMode,
                    sections = sections,
                    oneOffEntry = {
                        OtherEventRow(
                            onClick = { onOneOffAction(OneOffFormAction.Open) },
                            modifier = bounds.testTag(TestTags.ADD_PANE_OTHER_EVENT),
                        )
                    },
                    onPickEventType = onPickEventType,
                    onEditGroup = onEditGroup,
                    onAddPersonalEventType = onAddPersonalEventType,
                    onAddGroupEventType = onAddGroupEventType,
                    onCreateShifts = onCreateShifts,
                )
            }
        }
    }
}

@Composable
private fun EventTypePane(
    addMode: DayAddMode,
    sections: List<EventTypeSectionUi>,
    oneOffEntry: @Composable () -> Unit,
    onPickEventType: (eventType: EventTypeUi) -> Unit,
    onEditGroup: (groupId: String, groupName: String) -> Unit,
    onAddPersonalEventType: () -> Unit,
    onAddGroupEventType: (groupId: String) -> Unit,
    onCreateShifts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val personalSection = sections.firstOrNull { it.source is EventTypeSectionUi.Source.Personal }
    val groupSections = sections.filter { section ->
        val source = section.source as? EventTypeSectionUi.Source.Group ?: return@filter false
        section.events.isNotEmpty() || source.isAdmin
    }

    Column(
        modifier = modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (addMode) {
            DayAddMode.Disabled -> Unit
            // The user's shifts first: they are what the app is for, and a one-off is the exception.
            DayAddMode.Full -> Box(Modifier.testTag(TestTags.ADD_PANE_SHIFTS)) {
                CategoryArea(label = stringResource(Res.string.day_detail_personal_events)) {
                    val shifts = personalSection?.events.orEmpty()
                    if (shifts.isEmpty()) {
                        NoShiftsPrompt(onCreate = onCreateShifts)
                    } else {
                        EventTypeChipRow(
                            events = shifts,
                            onPick = onPickEventType,
                            trailing = { AddEventChip(onClick = onAddPersonalEventType) },
                        )
                    }
                }
            }
            is DayAddMode.GroupOnly -> if (groupSections.isEmpty()) {
                NoTypesPrompt(onManage = { onEditGroup(addMode.groupId.value, "") })
            } else {
                InfoBanner(text = stringResource(Res.string.event_group_only_banner))
            }
        }

        if (groupSections.isNotEmpty()) {
            CategoryArea(label = stringResource(Res.string.day_detail_group_events)) {
                groupSections.forEach { section ->
                    val group = section.source as? EventTypeSectionUi.Source.Group ?: return@forEach
                    GroupArea(title = group.groupName) {
                        EventTypeChipRow(
                            events = section.events,
                            onPick = onPickEventType,
                            trailing = if (group.isAdmin) {
                                { AddEventChip(onClick = { onAddGroupEventType(group.groupId) }) }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }

        if (addMode == DayAddMode.Full) oneOffEntry()
    }
}

/** The way to a one-off: there when it is needed, but never the first thing on the pane. */
@Composable
private fun OtherEventRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(0.dp)) {
        Text(
            text = stringResource(Res.string.add_pane_other_event),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun CategoryArea(
    label: String?,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (label != null) DayCategoryLabel(label)
        content()
    }
}

@Composable
private fun GroupArea(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            content()
        }
    }
}

@Composable
private fun EventTypeChipRow(
    events: List<EventTypeUi>,
    onPick: (EventTypeUi) -> Unit,
    trailing: @Composable (() -> Unit)? = null,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        events.forEach { eventType ->
            EventTypeChip(
                chipUi = eventType.chipUi,
                onClick = { onPick(eventType) },
                modifier = Modifier.testTag(TestTags.eventTypeChip(eventType.eventType.id)),
            )
        }
        trailing?.invoke()
    }
}

/** A group with no event types: nothing to add, and the way to fix it is the group's own screen. */
@Composable
private fun NoTypesPrompt(onManage: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(Res.string.event_group_no_types),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = onManage, contentPadding = PaddingValues(0.dp)) {
                Text(stringResource(Res.string.event_group_no_types_action))
            }
        }
    }
}

@Composable
private fun InfoBanner(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

// Previews. The labels are developer-facing, so they stay here rather than in composeResources.

private fun previewGroupType(
    group: String,
    groupName: String,
    name: String,
    acronym: String,
    hours: Pair<String, String>,
    color: String,
) = GroupEventType(
    id = EventTypeId("preview-$group-$acronym"),
    groupId = GroupId(group),
    groupName = groupName,
    name = name,
    acronym = acronym,
    description = null,
    startTime = hours.first,
    endTime = hours.second,
    swappable = true,
    defaultColor = color,
    userColor = null,
)

private fun previewPersonalType(name: String, acronym: String, color: String) = PersonalEventType(
    id = EventTypeId("preview-personal-$acronym"),
    name = name,
    color = color,
    acronym = acronym,
    description = null,
    startTime = null,
    endTime = null,
)

private fun EventType.previewUi() = EventTypeUi(
    chipUi = EventTypeChipUi(title = acronym ?: name, color = color.toComposeColorOr(Color.Gray)),
    eventType = this,
)

/** My personal types, a group I administer, and one where I am a plain member. */
internal val PreviewEventTypeSections = listOf(
    EventTypeSectionUi(
        source = EventTypeSectionUi.Source.Personal,
        events = listOf(
            previewPersonalType("Training", "T", "#00897B"),
            previewPersonalType("Holiday", "H", "#F9A825"),
        ).map { it.previewUi() },
    ),
    EventTypeSectionUi(
        source = EventTypeSectionUi.Source.Group("preview-emergency", "Emergency", isAdmin = true),
        events = listOf(
            previewGroupType("preview-emergency", "Emergency", "Morning", "M", "08:00" to "15:00", "#039BE5"),
            previewGroupType("preview-emergency", "Emergency", "Afternoon", "A", "15:00" to "22:00", "#FB8C00"),
            previewGroupType("preview-emergency", "Emergency", "Night", "N", "22:00" to "08:00", "#5E35B1"),
            previewGroupType("preview-emergency", "Emergency", "24 h on call", "OC", "08:00" to "08:00", "#E53935"),
        ).map { it.previewUi() },
    ),
    EventTypeSectionUi(
        source = EventTypeSectionUi.Source.Group("preview-icu", "Paediatric ICU", isAdmin = false),
        events = listOf(
            previewGroupType("preview-icu", "Paediatric ICU", "Extra shift", "X", "10:00" to "18:00", "#43A047"),
        ).map { it.previewUi() },
    ),
)

/** My own calendar: personal types and every group. */
@Preview
@Composable
fun DayDetailAddEventPreview() {
    PreviewAddEvent(DayAddMode.Full, PreviewEventTypeSections)
}

/** A group's calendar: only that group's types, under the banner that says so. */
@Preview
@Composable
fun DayDetailAddEventGroupOnlyPreview() {
    PreviewAddEvent(
        addMode = DayAddMode.GroupOnly(GroupId("preview-emergency")),
        sections = PreviewEventTypeSections.filter {
            (it.source as? EventTypeSectionUi.Source.Group)?.groupId == "preview-emergency"
        },
    )
}

/** An admin's group with no types yet, which offers to create the first one. */
@Preview
@Composable
fun DayDetailAddEventNoTypesPreview() {
    PreviewAddEvent(
        addMode = DayAddMode.Full,
        sections = listOf(
            PreviewEventTypeSections.first(),
            EventTypeSectionUi(
                source = EventTypeSectionUi.Source.Group("preview-new", "New ward", isAdmin = true),
                events = emptyList(),
            ),
        ),
    )
}

/** Writing a one-off event: the form has taken the pane over. */
@Preview
@Composable
fun DayDetailAddEventOneOffPreview() {
    val start = LocalDateTime(LocalDate(2026, 10, 3), LocalTime(17, 30))
    PreviewAddEvent(
        addMode = DayAddMode.Full,
        sections = PreviewEventTypeSections,
        oneOffForm = OneOffEventFormUi(
            name = "Dentista",
            start = start,
            end = LocalDateTime(start.date, LocalTime(18, 15)),
            color = EntityPalette.first(),
        ),
    )
}

@Composable
private fun PreviewAddEvent(
    addMode: DayAddMode,
    sections: List<EventTypeSectionUi>,
    oneOffForm: OneOffEventFormUi? = null,
) {
    PreviewTurniaTheme {
        Surface {
            DayDetailAddEvent(
                addMode = addMode,
                sections = sections,
                oneOffForm = oneOffForm,
                onOneOffAction = {},
                onPickEventType = {},
                onEditGroup = { _, _ -> },
                onAddPersonalEventType = {},
                onAddGroupEventType = {},
                modifier = Modifier.padding(20.dp),
            )
        }
    }
}
