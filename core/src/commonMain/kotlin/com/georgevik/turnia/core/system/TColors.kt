package com.georgevik.turnia.core.system

/**
 * Production palette backing `EntityPalette` — deliberately not in `MockData.kt`. Top-level
 * properties of a file share one `<clinit>` and initialise in declaration order, so an eager
 * property reading this one from higher up that file would see a null delegate (`by lazy` defers
 * the value, not the delegate). Its own file gets its own `<clinit>`, keyed on first access.
 */
val ALL_COLORS: List<String> = listOf(
    "#E53935", // red
    "#F4511E", // deep orange
    "#FB8C00", // orange
    "#F9A825", // yellow
    "#7CB342", // light green
    "#43A047", // green
    "#00897B", // teal
    "#00ACC1", // cyan
    "#039BE5", // light blue
    "#1E88E5", // blue
    "#3949AB", // indigo
    "#5E35B1", // deep purple
    "#8E24AA", // purple
    "#D81B60", // pink
)
