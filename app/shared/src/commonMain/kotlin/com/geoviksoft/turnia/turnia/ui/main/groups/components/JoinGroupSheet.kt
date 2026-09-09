package com.geoviksoft.turnia.ui.main.groups.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.geoviksoft.turnia.ui.system.keyboardAware
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.groups_join_action
import turnia.app.shared.generated.resources.groups_join_body
import turnia.app.shared.generated.resources.groups_join_hint
import turnia.app.shared.generated.resources.groups_join_title

@Composable
fun JoinGroupSheet(
    code: String,
    inProgress: Boolean,
    onCodeChanged: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .keyboardAware()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(Res.string.groups_join_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(Res.string.groups_join_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = code,
            onValueChange = onCodeChanged,
            modifier = Modifier.fillMaxWidth(),
            enabled = !inProgress,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
            keyboardOptions = KeyboardOptions.Default.copy(
                capitalization = KeyboardCapitalization.Characters,
            ),
            placeholder = { Text(stringResource(Res.string.groups_join_hint)) },
        )
        Button(
            onClick = onSubmit,
            enabled = !inProgress && code.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (inProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(
                    text = stringResource(Res.string.groups_join_action),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
