package com.geoviksoft.turnia.ui.shiftsetup.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.geoviksoft.turnia.ui.shiftsetup.model.CustomShiftForm
import com.geoviksoft.turnia.ui.shiftsetup.model.ShiftPreset
import com.geoviksoft.turnia.ui.shiftsetup.model.ShiftRowUi
import com.geoviksoft.turnia.ui.system.PreviewTurniaTheme
import com.geoviksoft.turnia.ui.system.TestTags
import com.geoviksoft.turnia.ui.system.color.EntityPalette
import com.geoviksoft.turnia.ui.system.components.time.TTimeField
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.event_type_field_acronym
import turnia.app.shared.generated.resources.event_type_field_end
import turnia.app.shared.generated.resources.event_type_field_name
import turnia.app.shared.generated.resources.event_type_field_start
import turnia.app.shared.generated.resources.shift_setup_custom_add
import turnia.app.shared.generated.resources.shift_setup_custom_cancel
import turnia.app.shared.generated.resources.shift_setup_required

@Composable
fun CreateInlineShiftRow(
    form: CustomShiftForm,
    onChange: (CustomShiftForm) -> Unit,
    onAdd: () -> Unit,
    onCancel: () -> Unit,
) {
    val required = stringResource(Res.string.shift_setup_required)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = form.name,
                onValueChange = { onChange(form.copy(name = it)) },
                label = { Text(stringResource(Res.string.event_type_field_name)) },
                isError = form.nameMissing,
                supportingText = if (form.nameMissing) ({ Text(required) }) else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.weight(2f).testTag(TestTags.SHIFT_SETUP_CUSTOM_NAME),
            )
            OutlinedTextField(
                value = form.acronym,
                onValueChange = { onChange(form.copy(acronym = it)) },
                label = { Text(stringResource(Res.string.event_type_field_acronym)) },
                isError = form.acronymMissing,
                supportingText = if (form.acronymMissing) ({ Text(required) }) else null,
                singleLine = true,
                maxLines = 1,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.weight(1f).testTag(TestTags.SHIFT_SETUP_CUSTOM_ACRONYM),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TTimeField(
                value = form.start,
                onValueChange = { onChange(form.copy(start = it)) },
                label = stringResource(Res.string.event_type_field_start),
                modifier = Modifier.weight(1f).testTag(TestTags.SHIFT_SETUP_CUSTOM_START),
            )
            TTimeField(
                value = form.end,
                onValueChange = { onChange(form.copy(end = it)) },
                label = stringResource(Res.string.event_type_field_end),
                modifier = Modifier.weight(1f).testTag(TestTags.SHIFT_SETUP_CUSTOM_END),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.align(Alignment.End)
        ) {
            TextButton(onClick = onCancel) { Text(stringResource(Res.string.shift_setup_custom_cancel)) }
            Button(onClick = onAdd) { Text(stringResource(Res.string.shift_setup_custom_add)) }
        }
    }
}


@Preview
@Composable
fun CreateInlineShiftRowPreview() {
    val row = ShiftRowUi(
        id = "id",
        preset = ShiftPreset.Afternoon,
        name = "name",
        acronym = "acronym",
        start = "start",
        end = "end",
        color = EntityPalette[0],
        selected = true
    )

    PreviewTurniaTheme {
        CreateInlineShiftRow(
            form = CustomShiftForm(
                name = "",
                acronym = "",
                start = "",
                end = "",
                nameMissing = false,
                acronymMissing = false,
            ),
            onChange = {},
            onAdd = {},
            onCancel = {},
        )
    }
}
