package com.georgevik.turnia.navigation

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/** The nav host the content is currently rendered in — Main's tab stacks, or the root stack. */
val LocalNavigator: ProvidableCompositionLocal<Navigator> = staticCompositionLocalOf {
    error("LocalNavigator not provided — this content must be hosted under a NavDisplay")
}

/**
 * The root back stack, reachable from anywhere. Content inside Main uses it to open destinations
 * that cover the whole screen (bottom bar included), which [LocalNavigator] cannot do there.
 */
val LocalRootNavigator: ProvidableCompositionLocal<Navigator> = staticCompositionLocalOf {
    error("LocalRootNavigator not provided — this content must be hosted under App")
}
