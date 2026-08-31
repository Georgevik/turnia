package com.georgevik.turnia.ui.components.daydetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import turnia.app.shared.generated.resources.event_add_custom
import turnia.app.shared.generated.resources.event_group_only_banner
import turnia.app.shared.generated.resources.event_no_predefined_body
import turnia.app.shared.generated.resources.event_no_predefined_title

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
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        if (groupOnly) {
            InfoBanner(text = stringResource(Res.string.event_group_only_banner))
            Spacer(Modifier.size(12.dp))
        }

        if (sections.all { it.events.isEmpty() }) {
            // The group-only banner already explains the empty state for a group;
            // only the personal-calendar case needs the "create one" prompt.
            if (!groupOnly) NoPredefinedBanner()
        } else {
            sections.forEach { section ->
                if (section.events.isEmpty()) return@forEach

                // The group name is redundant when the whole sheet is that one group.
                if (!groupOnly && section.groupId != null && section.groupName != null) {
                    DaySectionHeader(
                        title = section.groupName,
                        onEdit = { onEditGroup(section.groupId, section.groupName) },
                    )
                }
                FlowRow(
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    section.events.forEach { predefined ->
                        PredefinedEventChip(
                            predefined = predefined,
                            onClick = { onPickPredefined(predefined) })
                    }
                }
            }
        }

        // A custom event is a personal event, so it only belongs on my own calendar.
        if (addMode is DayAddMode.Full) {
            OutlinedButton(
                onClick = onAddCustom,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(Res.string.event_add_custom))
            }
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
