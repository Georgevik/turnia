package com.geoviksoft.turnia.ui.components.daydetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.geoviksoft.turnia.ui.components.daydetail.DayAddMode
import com.geoviksoft.turnia.ui.components.daydetail.model.EventTypeSectionUi
import com.geoviksoft.turnia.ui.components.daydetail.model.EventTypeUi
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.day_detail_group_events
import turnia.app.shared.generated.resources.day_detail_personal_events
import turnia.app.shared.generated.resources.event_group_no_types
import turnia.app.shared.generated.resources.event_group_no_types_action
import turnia.app.shared.generated.resources.event_group_only_banner

@Composable
fun DayDetailAddEvent(
    addMode: DayAddMode,
    sections: List<EventTypeSectionUi>,
    onPickEventType: (eventType: EventTypeUi) -> Unit,
    onEditGroup: (groupId: String, groupName: String) -> Unit,
    onAddPersonalEventType: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filled = sections.filter { it.events.isNotEmpty() }
    val personalSection = filled.firstOrNull { it.source is EventTypeSectionUi.Source.Personal }
    val groupSections = filled.filter { it.source is EventTypeSectionUi.Source.Group }

    Column(
        modifier = modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (addMode) {
            DayAddMode.Disabled -> Unit
            DayAddMode.Full -> CategoryArea(label = stringResource(Res.string.day_detail_personal_events)) {
                EventTypeChipRow(
                    events = personalSection?.events.orEmpty(),
                    onPick = onPickEventType,
                    trailing = { AddEventChip(onClick = onAddPersonalEventType) },
                )
            }
            is DayAddMode.GroupOnly -> if (groupSections.isEmpty()) {
                // An empty section is filtered out above, so a group whose types have all been
                // deleted would otherwise render an add pane with nothing in it and no way out.
                NoTypesPrompt(onManage = { onEditGroup(addMode.groupId.value, "") })
            } else {
                InfoBanner(text = stringResource(Res.string.event_group_only_banner))
            }
        }

        if (groupSections.isNotEmpty()) {
            CategoryArea(label = stringResource(Res.string.day_detail_group_events)) {
                groupSections.forEach { section ->
                    val group = section.source as? EventTypeSectionUi.Source.Group ?: return@forEach
                    GroupArea(
                        title = group.groupName,
                        onEdit = { onEditGroup(group.groupId, group.groupName) },
                    ) {
                        EventTypeChipRow(section.events, onPickEventType)
                    }
                }
            }
        }
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
    onEdit: () -> Unit,
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
            DaySectionHeader(title = title, onEdit = onEdit)
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
            EventTypeChip(chipUi = eventType.chipUi, onClick = { onPick(eventType) })
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
