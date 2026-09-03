package com.georgevik.turnia.ui.system.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.georgevik.turnia.ui.system.toTimeInput
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.time_field_hint

/**
 * Time field: the user types digits and reads `HH:mm` — the colon is never typed. [onValueChange]
 * receives the formatted text, so the caller holds it as-is and hands it back as [value]. Complete
 * a partial entry with [com.georgevik.turnia.ui.system.toTimeOrNull] before saving it.
 */
@Composable
fun TTimeField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.toTimeInput()) },
        label = { Text(label) },
        placeholder = { Text(stringResource(Res.string.time_field_hint)) },
        singleLine = true,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
    )
}
