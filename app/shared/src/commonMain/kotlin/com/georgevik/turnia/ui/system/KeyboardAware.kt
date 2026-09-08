package com.georgevik.turnia.ui.system

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager

/**
 * Lays a form out above the keyboard instead of behind it, and dismisses the keyboard on a tap
 * that no field claimed.
 *
 * The tap is not a convenience. `KeyboardType.Number` is iOS's `numberPad`, which draws no return
 * key — `ImeAction.Done` does not add one — so on a time field it is the only way to close the
 * keyboard: iOS has no back gesture to fall back on the way Android does.
 *
 * Goes **outside** the scroll, so the viewport shrinks and whatever sits at the bottom of the form
 * can be scrolled clear of the keyboard.
 */
@Composable
fun Modifier.keyboardAware(): Modifier {
    val focusManager = LocalFocusManager.current
    return imePadding().pointerInput(Unit) {
        detectTapGestures { focusManager.clearFocus() }
    }
}

/**
 * The same, for a `Scaffold`'s content, applying its [contentPadding] first.
 *
 * `Scaffold` reports its insets without consuming them, so they are consumed here: otherwise
 * `imePadding` would count the navigation bar a second time and leave a gap its height above the
 * keyboard.
 */
@Composable
fun Modifier.keyboardAware(contentPadding: PaddingValues): Modifier =
    padding(contentPadding).consumeWindowInsets(contentPadding).keyboardAware()
