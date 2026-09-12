package com.geoviksoft.turnia.ui.main.preferences.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geoviksoft.turnia.ui.system.AppLanguage
import com.geoviksoft.turnia.ui.system.PreviewTurniaTheme
import com.geoviksoft.turnia.ui.system.components.TListItem
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.language_english
import turnia.app.shared.generated.resources.language_spanish
import turnia.app.shared.generated.resources.language_system
import turnia.app.shared.generated.resources.preferences_section_language

@Composable
fun LanguageSheet(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(Res.string.preferences_section_language),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            AppLanguage.entries.forEach { language ->
                TListItem(
                    title = stringResource(language.label),
                    onClick = { onSelect(language) },
                    leading = { LanguageFlag(language) },
                    trailing = { RadioButton(selected = language == selected, onClick = { onSelect(language) }) },
                )
            }
        }
    }
}

/**
 * The language's flag, drawn as an emoji so it needs no asset and renders in colour on both
 * platforms. Following the device has no country, so it gets the device instead.
 */
@Composable
fun LanguageFlag(language: AppLanguage, modifier: Modifier = Modifier) {
    // An icon's size, so a flag lines up with the icons in the rows around it.
    Box(modifier.size(24.dp), contentAlignment = Alignment.Center) {
        val flag = language.flag
        if (flag == null) {
            Icon(
                imageVector = Icons.Default.Smartphone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
            )
        } else {
            Text(text = flag, fontSize = 20.sp)
        }
    }
}

val AppLanguage.label: StringResource
    get() = when (this) {
        AppLanguage.System -> Res.string.language_system
        AppLanguage.English -> Res.string.language_english
        AppLanguage.Spanish -> Res.string.language_spanish
    }

// British: that is the English the strings are written in.
private val AppLanguage.flag: String?
    get() = when (this) {
        AppLanguage.System -> null
        AppLanguage.English -> "🇬🇧"
        AppLanguage.Spanish -> "🇪🇸"
    }

@Preview
@Composable
private fun LanguageSheetPreview() {
    PreviewTurniaTheme {
        LanguageSheet(selected = AppLanguage.Spanish, onSelect = {})
    }
}
