package com.geoviksoft.turnia.ui.components.movetogroup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geoviksoft.turnia.core.domain.model.EventTypeId
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.MoveError
import com.geoviksoft.turnia.core.domain.model.MoveScope
import com.geoviksoft.turnia.ui.components.event.AcronymChip
import com.geoviksoft.turnia.ui.system.PreviewTurniaTheme
import com.geoviksoft.turnia.ui.system.TestTags
import com.geoviksoft.turnia.ui.system.color.readableTextColor
import com.geoviksoft.turnia.ui.system.components.TListItem
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.move_done
import turnia.app.shared.generated.resources.move_done_moved
import turnia.app.shared.generated.resources.move_done_skipped
import turnia.app.shared.generated.resources.move_done_title
import turnia.app.shared.generated.resources.move_error_day_taken
import turnia.app.shared.generated.resources.move_error_failed
import turnia.app.shared.generated.resources.move_moving
import turnia.app.shared.generated.resources.move_pick_group_title
import turnia.app.shared.generated.resources.move_pick_type_title
import turnia.app.shared.generated.resources.move_scope_all
import turnia.app.shared.generated.resources.move_scope_body
import turnia.app.shared.generated.resources.move_scope_one
import turnia.app.shared.generated.resources.move_scope_title

/** The content of the bottom sheet a "Move to a group" opens; [onClose] dismisses it. */
@Composable
fun MoveToGroupSheet(
    request: MoveRequest,
    onClose: () -> Unit,
    viewModel: MoveToGroupViewModel = koinViewModel(key = "move-${request.eventId.value}") {
        parametersOf(request)
    },
) {
    val step by viewModel.step.collectAsStateWithLifecycle()
    MoveToGroupContent(
        step = step,
        onPickGroup = viewModel::pickGroup,
        onPickType = viewModel::pickType,
        onPickScope = viewModel::pickScope,
        onClose = onClose,
    )
}

@Composable
private fun MoveToGroupContent(
    step: MoveStep,
    onPickGroup: (GroupId) -> Unit,
    onPickType: (EventTypeId) -> Unit,
    onPickScope: (MoveScope) -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TestTags.MOVE_SHEET)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (step) {
            MoveStep.Loading, MoveStep.Moving -> Busy(moving = step == MoveStep.Moving)

            is MoveStep.PickGroup -> {
                Title(stringResource(Res.string.move_pick_group_title))
                // A plain Column: nobody belongs to enough groups for a lazy list to matter.
                step.groups.forEach { group ->
                    TListItem(
                        title = group.name,
                        onClick = { onPickGroup(group.id) },
                        leading = { Dot(group.color) },
                    )
                }
            }

            is MoveStep.PickType -> {
                Title(stringResource(Res.string.move_pick_type_title, step.group.name))
                step.types.forEach { type ->
                    TListItem(
                        title = type.name,
                        modifier = Modifier.testTag(TestTags.moveType(type.id)),
                        onClick = { onPickType(type.id) },
                        leading = {
                            AcronymChip(
                                acronym = type.acronym?.takeIf { it.isNotBlank() } ?: type.name.take(1),
                                background = type.color,
                                textColor = type.color.readableTextColor(),
                            )
                        },
                    )
                }
            }

            is MoveStep.PickScope -> {
                Title(stringResource(Res.string.move_scope_title))
                Body(pluralStringResource(Res.plurals.move_scope_body, step.count, step.typeName, step.count))
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onPickScope(MoveScope.All) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.move_scope_all))
                }
                OutlinedButton(onClick = { onPickScope(MoveScope.One) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.move_scope_one))
                }
            }

            is MoveStep.Done -> {
                Title(stringResource(Res.string.move_done_title, step.groupName))
                Body(step.summary())
                CloseButton(onClose)
            }

            is MoveStep.Failed -> {
                Body(
                    stringResource(
                        when (step.error) {
                            MoveError.DayTaken -> Res.string.move_error_day_taken
                            MoveError.Failed -> Res.string.move_error_failed
                        }
                    )
                )
                CloseButton(onClose)
            }
        }
    }
}

/** "12 moved, 2 already had a shift" — the second half only when a day was skipped. */
@Composable
private fun MoveStep.Done.summary(): String {
    val moved = pluralStringResource(Res.plurals.move_done_moved, moved, moved)
    if (skipped == 0) return moved
    return "$moved, ${pluralStringResource(Res.plurals.move_done_skipped, skipped, skipped)}"
}

@Composable
private fun Title(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
}

@Composable
private fun Body(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun CloseButton(onClose: () -> Unit) {
    Spacer(Modifier.height(8.dp))
    Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.move_done))
    }
}

@Composable
private fun Busy(moving: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp))
        if (moving) Body(stringResource(Res.string.move_moving))
    }
}

@Composable
private fun Dot(color: Color) {
    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(14.dp).clip(CircleShape).background(color))
    }
}

private val previewGroup = MoveGroupUi(GroupId("u"), "Urgencias", Color(0xFFE53935))

@Composable
private fun PreviewStep(step: MoveStep) {
    PreviewTurniaTheme {
        MoveToGroupContent(step = step, onPickGroup = {}, onPickType = {}, onPickScope = {}, onClose = {})
    }
}

@Preview
@Composable
private fun MoveToGroupPickGroupPreview() = PreviewStep(
    MoveStep.PickGroup(listOf(previewGroup, MoveGroupUi(GroupId("p"), "Planta 3", Color(0xFF43A047))))
)

@Preview
@Composable
private fun MoveToGroupPickTypePreview() = PreviewStep(
    MoveStep.PickType(
        previewGroup,
        listOf(
            MoveTypeUi(EventTypeId("m"), "Morning", "M", Color(0xFF039BE5)),
            MoveTypeUi(EventTypeId("n"), "Night", "N", Color(0xFF5E35B1)),
        ),
    )
)

@Preview
@Composable
private fun MoveToGroupPickScopePreview() = PreviewStep(MoveStep.PickScope("Mañana", 14))

@Preview
@Composable
private fun MoveToGroupDonePreview() = PreviewStep(MoveStep.Done("Urgencias", moved = 12, skipped = 2))

@Preview
@Composable
private fun MoveToGroupFailedPreview() = PreviewStep(MoveStep.Failed(MoveError.DayTaken))
