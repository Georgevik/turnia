package com.georgevik.turnia.ui.components.daydetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.georgevik.turnia.ui.components.daydetail.DayAddMode
import com.georgevik.turnia.ui.components.daydetail.model.PredefinedEventUi
import com.georgevik.turnia.ui.components.daydetail.model.PredefinedSectionUi
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.event_add_custom_chip
import turnia.app.shared.generated.resources.event_group_only_banner
import turnia.app.shared.generated.resources.event_no_predefined_body
import turnia.app.shared.generated.resources.event_no_predefined_title
import turnia.app.shared.generated.resources.event_section_groups
import turnia.app.shared.generated.resources.event_section_mine

@Composable
fun DayDetailAddEvent(
    addMode: DayAddMode,
    sections: List<PredefinedSectionUi>,
    onPickPredefined: (predefined: PredefinedEventUi) -> Unit,
    onEditGroup: (groupId: String, groupName: String) -> Unit,
    onAddCustom: () -> Unit,
    modifier: Modifier = Modifier
) {
    val groupOnly = addMode is DayAddMode.GroupOnly
    // A custom event is a personal event, so it only belongs on my own calendar.
    val canAddCustom = addMode is DayAddMode.Full

    val personalEvents = sections.firstOrNull { it.groupId == null }?.events.orEmpty()
    val groupSections = sections.filter {
        it.groupId != null && it.groupName != null && it.events.isNotEmpty()
    }

    Column(
        modifier = modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (groupOnly) {
            InfoBanner(text = stringResource(Res.string.event_group_only_banner))
            // On a group-only sheet the group name is redundant, so drop the card header.
            groupSections.forEach { section ->
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    section.events.forEach { predefined ->
                        PredefinedEventChip(
                            predefined = predefined,
                            onClick = { onPickPredefined(predefined) })
                    }
                }
            }
            return@Column
        }

        if (canAddCustom) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle(stringResource(Res.string.event_section_mine))
                if (personalEvents.isEmpty() && groupSections.isEmpty()) {
                    NoPredefinedBanner()
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    personalEvents.forEach { predefined ->
                        PredefinedEventChip(
                            predefined = predefined,
                            onClick = { onPickPredefined(predefined) })
                    }
                    AddCustomChip(onClick = onAddCustom)
                }
            }
        }

        if (canAddCustom && groupSections.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        if (groupSections.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle(stringResource(Res.string.event_section_groups))
                groupSections.forEach { section ->
                    GroupEventCard(
                        groupName = section.groupName!!,
                        events = section.events,
                        onEdit = { onEditGroup(section.groupId!!, section.groupName) },
                        onPickPredefined = onPickPredefined,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun GroupEventCard(
    groupName: String,
    events: List<PredefinedEventUi>,
    onEdit: () -> Unit,
    onPickPredefined: (predefined: PredefinedEventUi) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DaySectionHeader(title = groupName, onEdit = onEdit)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                events.forEach { predefined ->
                    PredefinedEventChip(
                        predefined = predefined,
                        onClick = { onPickPredefined(predefined) })
                }
            }
        }
    }
}

@Composable
private fun AddCustomChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(Res.string.event_add_custom_chip),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}


@Composable
private fun NoPredefinedBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Default.Info, contentDescription = null)
            Column {
                Text(
                    text = stringResource(Res.string.event_no_predefined_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(Res.string.event_no_predefined_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
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
