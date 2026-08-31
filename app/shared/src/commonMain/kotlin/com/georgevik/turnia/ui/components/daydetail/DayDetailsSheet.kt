package com.georgevik.turnia.ui.components.daydetail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.georgevik.turnia.ui.components.calendar.model.CalendarEventUi
import com.georgevik.turnia.ui.components.daydetail.components.DayDetailAddEvent
import com.georgevik.turnia.ui.components.daydetail.components.DayDetailHeader
import com.georgevik.turnia.ui.components.daydetail.components.DayEventRow
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.event_details_empty

@Composable
fun DayDetailsSheet(
    date: LocalDate,
    events: List<CalendarEventUi>,
    openEditTypeScreen: (groupId: String, groupName: String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DayDetailsSheetViewModel = koinViewModel(key = date.toString()) {
        parametersOf(date)
    },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is DayDetailsSheetUiEvent.EditGroup ->
                    openEditTypeScreen(event.groupId, event.groupName)
            }
        }
    }
    var adding by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 560.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 16.dp),
    ) {
        DayDetailHeader(
            date = date,
            eventCount = events.size,
            adding = adding,
            onToggleAdd = { adding = !adding },
        )

        Spacer(Modifier.height(16.dp))

        Box {
            AnimatedContent(adding, transitionSpec = {
                fadeIn() togetherWith fadeOut(animationSpec = tween(90))
            }) { isAdding ->
                if (isAdding) {
                    DayDetailAddEvent(
                        sections = uiState.predefinedSections,
                        onPickPredefined = { predefined ->
                            viewModel.addPredefinedEvent(predefined.id)
                            onClose()
                        },
                        onEditGroup = { groupId, groupName ->
                            viewModel.editGroup(groupId, groupName)
                        },
                        onAddCustom = {
                            viewModel.addCustomEvent()
                            onClose()
                        },
                    )

                } else if (events.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.event_details_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                } else {
                    Column {
                        events.forEachIndexed { index, event ->
                            if (index > 0) Spacer(Modifier.height(12.dp))
                            DayEventRow(event = event)
                        }
                    }
                }
            }


        }
    }
}
