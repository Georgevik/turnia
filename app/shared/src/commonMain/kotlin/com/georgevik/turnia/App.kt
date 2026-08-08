package com.georgevik.turnia

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.georgevik.turnia.navigation.TurniaNavDisplay

@Composable
@Preview
fun App() {
    MaterialTheme {
        TurniaNavDisplay()
    }
}
