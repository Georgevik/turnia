package com.geoviksoft.turnia.ui.main.profile.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.profile_photo_choose_animal
import turnia.app.shared.generated.resources.profile_photo_remove
import turnia.app.shared.generated.resources.profile_photo_upload

/** What the avatar offers: a photo, the animal and its colour, and — with a photo — taking it off. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarMenuSheet(
    hasPhoto: Boolean,
    onUploadPhoto: () -> Unit,
    onChooseAnimal: () -> Unit,
    onRemovePhoto: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Option(Icons.Default.Image, Res.string.profile_photo_upload, onUploadPhoto)
            Option(Icons.Default.Face, Res.string.profile_photo_choose_animal, onChooseAnimal)
            if (hasPhoto) {
                Option(Icons.Default.Delete, Res.string.profile_photo_remove, onRemovePhoto, destructive = true)
            }
        }
    }
}

@Composable
private fun Option(icon: ImageVector, label: StringResource, onClick: () -> Unit, destructive: Boolean = false) {
    val color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    ListItem(
        headlineContent = { Text(stringResource(label), color = color) },
        leadingContent = { Icon(icon, contentDescription = null, tint = color) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
