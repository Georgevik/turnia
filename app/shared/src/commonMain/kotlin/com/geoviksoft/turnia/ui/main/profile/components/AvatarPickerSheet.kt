package com.geoviksoft.turnia.ui.main.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.geoviksoft.turnia.ui.system.PreviewTurniaTheme
import com.geoviksoft.turnia.ui.system.avatar.AnimalIconIds
import com.geoviksoft.turnia.ui.system.avatar.animalIcon
import com.geoviksoft.turnia.ui.system.color.EntityPalette
import com.geoviksoft.turnia.ui.system.color.toComposeColorOrNull
import com.geoviksoft.turnia.ui.system.color.toHex
import com.geoviksoft.turnia.ui.system.components.ColorSwatchPicker
import com.geoviksoft.turnia.ui.system.components.UserAvatar
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.profile_avatar_animal
import turnia.app.shared.generated.resources.profile_avatar_color
import turnia.app.shared.generated.resources.profile_avatar_done
import turnia.app.shared.generated.resources.profile_avatar_title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarPickerSheet(
    animalIconId: String?,
    backgroundColor: String?,
    onAnimalPicked: (String) -> Unit,
    onColorPicked: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            AvatarPickerContent(
                animalIconId = animalIconId,
                backgroundColor = backgroundColor,
                onAnimalPicked = onAnimalPicked,
                onColorPicked = onColorPicked,
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun AvatarPickerContent(
    animalIconId: String?,
    backgroundColor: String?,
    onAnimalPicked: (String) -> Unit,
    onColorPicked: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = backgroundColor?.toComposeColorOrNull() ?: EntityPalette.first()

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(Res.string.profile_avatar_title),
            style = MaterialTheme.typography.titleMedium,
        )

        UserAvatar(
            background = background,
            animalIcon = animalIcon(animalIconId),
            modifier = Modifier.size(72.dp).align(Alignment.CenterHorizontally),
        )

        Text(
            text = stringResource(Res.string.profile_avatar_color),
            style = MaterialTheme.typography.labelLarge,
        )
        ColorSwatchPicker(
            colors = EntityPalette,
            selected = background,
            onPick = { color -> onColorPicked(color.toHex()) },
        )

        Text(
            text = stringResource(Res.string.profile_avatar_animal),
            style = MaterialTheme.typography.labelLarge,
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(56.dp),
            modifier = Modifier.fillMaxWidth().height(240.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(AnimalIconIds, key = { it }) { id ->
                AnimalOption(
                    id = id,
                    background = background,
                    isSelected = id == animalIconId,
                    onPick = { onAnimalPicked(id) },
                )
            }
        }

        Button(
            onClick = onDismiss,
            shape = RoundedCornerShape(percent = 50),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text(stringResource(Res.string.profile_avatar_done))
        }
    }

}

@Composable
private fun AnimalOption(
    id: String,
    background: Color,
    isSelected: Boolean,
    onPick: () -> Unit,
) {
    UserAvatar(
        background = if (isSelected) background else MaterialTheme.colorScheme.surfaceVariant,
        animalIcon = animalIcon(id),
        modifier = Modifier
            .size(56.dp)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onPick),
    )
}

@Preview
@Composable
fun AvatarPickerSheetPreview() {
    PreviewTurniaTheme {
        AvatarPickerContent(
            modifier = Modifier.background(Color.White),
            animalIconId = "cat",
            backgroundColor = null,
            onAnimalPicked = {},
            onColorPicked = {},
            onDismiss = {},
        )
    }
}
