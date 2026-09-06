package com.georgevik.turnia.ui.main.groups.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.georgevik.turnia.core.domain.model.GroupId
import com.georgevik.turnia.ui.main.groups.model.JoinRequestRowUi
import com.georgevik.turnia.ui.system.TurniaTheme
import com.georgevik.turnia.ui.system.components.Avatar
import com.georgevik.turnia.ui.system.components.TListItem
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.groups_request_cancel
import turnia.app.shared.generated.resources.groups_request_dismiss
import turnia.app.shared.generated.resources.groups_request_pending
import turnia.app.shared.generated.resources.groups_request_rejected
import turnia.app.shared.generated.resources.groups_request_unknown_group

@Composable
fun JoinRequestCard(request: JoinRequestRowUi, onDismiss: () -> Unit) {
    TListItem(
        title = request.groupName.ifBlank {
            stringResource(Res.string.groups_request_unknown_group)
        },
        subtitle = stringResource(
            if (request.isPending) Res.string.groups_request_pending
            else Res.string.groups_request_rejected
        ),
        leading = {
            Avatar(
                background = if (request.isPending) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.error,
                icon = if (request.isPending) Icons.Default.HourglassTop else Icons.Default.Block,
            )
        },
        trailing = {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(
                        if (request.isPending) Res.string.groups_request_cancel
                        else Res.string.groups_request_dismiss
                    ),
                )
            }
        },
    )
}

@Preview
@Composable
private fun JoinRequestCardPreview() {
    TurniaTheme {
        JoinRequestCard(
            request = JoinRequestRowUi(
                groupId = GroupId("1"),
                groupName = "Urgencias",
                isPending = true,
            ),
            onDismiss = {},
        )
    }
}
