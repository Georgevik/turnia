package com.georgevik.turnia

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.georgevik.turnia.navigation.TurniaNavDisplay
import com.georgevik.turnia.ui.system.TurniaTheme

@Composable
@Preview
fun App() {
    TurniaTheme {
        TurniaNavDisplay()
    }
}
