package com.georgevik.turnia.ui.main.groups.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.group_detail_create
import turnia.app.shared.generated.resources.groups_join_menu

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GroupsFabMenu(
    onJoin: () -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    FloatingActionButtonMenu(
        expanded = expanded,
        modifier = modifier,
        button = {
            ToggleFloatingActionButton(
                checked = expanded,
                onCheckedChange = { expanded = it },
            ) {
                val icon by remember {
                    derivedStateOf {
                        if (checkedProgress > 0.5f) Icons.Default.Close else Icons.Default.Add
                    }
                }
                with(ToggleFloatingActionButtonDefaults) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.animateIcon({ checkedProgress }),
                    )
                }
            }
        },
    ) {
        FloatingActionButtonMenuItem(
            onClick = {
                expanded = false
                onJoin()
            },
            icon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
            text = { Text(stringResource(Res.string.groups_join_menu)) },
        )
        FloatingActionButtonMenuItem(
            onClick = {
                expanded = false
                onCreate()
            },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text(stringResource(Res.string.group_detail_create)) },
        )
    }
}
