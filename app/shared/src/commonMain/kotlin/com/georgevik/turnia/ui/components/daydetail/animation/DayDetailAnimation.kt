package com.georgevik.turnia.ui.components.daydetail.animation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.Modifier

fun Modifier.fadeInContent(scope: AnimatedVisibilityScope): Modifier =
    this.then(
        with(scope) {
            Modifier.animateEnterExit(
                enter = fadeIn(),
                exit = fadeOut(),
            )
        },
    )
