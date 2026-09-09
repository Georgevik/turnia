package com.geoviksoft.turnia.ui.main.groups.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.ui.main.groups.model.JoinRequestRowUi
import com.geoviksoft.turnia.ui.system.TurniaTheme
import com.geoviksoft.turnia.ui.system.components.Avatar
import com.geoviksoft.turnia.ui.system.components.TListItem
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.groups_request_pending
import turnia.app.shared.generated.resources.groups_request_rejected
import turnia.app.shared.generated.resources.groups_request_unknown_group

@Composable
fun JoinRequestCard(request: JoinRequestRowUi) {
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
        )
    }
}
