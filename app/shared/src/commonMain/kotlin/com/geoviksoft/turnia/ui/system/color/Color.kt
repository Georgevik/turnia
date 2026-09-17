package com.geoviksoft.turnia.ui.system.color

import androidx.compose.ui.graphics.Color

// Turnia brand colors, grouped by hue. Within a group a higher number means
// more intensity (darker/deeper). The role mapping lives in Theme.kt.

// Neutral extremes
val White = Color(0xFFFFFFFF)

// Camel — the logo's ground, as on the launcher icon; the logo's T carries its own black
val Camel = Color(0xFFF3EFE9)

// The debug build's ground, on its launcher icon and logo alike, so it is never mistaken for the Play one
val DebugOrange = Color(0xFFE8590C)

// Grey — cool neutral (surfaces & text)
val Grey50 = Color(0xFFF8F9FC)
val Grey100 = Color(0xFFF2F4F6)
val Grey150 = Color(0xFFEFF1F3)
val Grey200 = Color(0xFFECEEF0)
val Grey250 = Color(0xFFE7E8EB)
val Grey300 = Color(0xFFE1E2E5)
val Grey400 = Color(0xFFD8DADD)
val Grey450 = Color(0xFFBBCAC6)
val Grey600 = Color(0xFF6C7A77)
val Grey700 = Color(0xFF3C4947)
val Grey800 = Color(0xFF2E3133)
val Grey900 = Color(0xFF191C1E)

// Teal — primary
val Teal200 = Color(0xFF4FDBC8)
val Teal300 = Color(0xFF14B8A6)
val Teal400 = Color(0xFF00897A)
val Teal500 = Color(0xFF006B5F)
val Teal700 = Color(0xFF00524A)
val Teal800 = Color(0xFF00423B)

// Slate — secondary
val Slate200 = Color(0xFFC5E5F4)
val Slate500 = Color(0xFF4F6D7A)
val Slate600 = Color(0xFF496774)
val Slate700 = Color(0xFF33505C)

// Amber — tertiary
val Amber300 = Color(0xFFD4984D)
val Amber500 = Color(0xFFC0873E)
val Amber700 = Color(0xFF6E4A1E)
val Amber800 = Color(0xFF543200)

// Green — success. Not a Material role: a covered shift has to read as settled at a glance, and
val Green100 = Color(0xFFDCF3E3)
val Green200 = Color(0xFFA3DDB5)
val Green700 = Color(0xFF1E6B3C)
val Green900 = Color(0xFF15311F)

// Red — error
val Red100 = Color(0xFFFFDAD6)
val Red200 = Color(0xFFFFB4AB)
val Red500 = Color(0xFFBA1A1A)
val Red800 = Color(0xFF93000A)
val Red900 = Color(0xFF690005)

// Dark neutrals — teal-tinted surfaces for the dark theme. A stepped elevation
// ladder (higher number = brighter/more elevated) so cards, sheets and menus
// separate from the page instead of blending into one flat near-black. Kept
// slightly above pure black to avoid the oppressive, low-contrast look.
val Ink900 = Color(0xFF0E1211) // background / surfaceDim
val Ink850 = Color(0xFF0A0F0E) // surfaceContainerLowest
val Ink800 = Color(0xFF161B1A) // surface / surfaceContainerLow
val Ink750 = Color(0xFF1B211F) // surfaceContainer
val Ink700 = Color(0xFF252B29) // surfaceContainerHigh
val Ink650 = Color(0xFF303634) // surfaceContainerHighest / surfaceVariant
val Ink600 = Color(0xFF353B39) // surfaceBright

// Dark neutrals — foreground tones on the ink surfaces.
val Mist100 = Color(0xFFE1E3E1) // onBackground / onSurface (soft white)
val Mist300 = Color(0xFFBEC9C4) // onSurfaceVariant (muted)
val Mist500 = Color(0xFF889390) // outline
val Mist700 = Color(0xFF3F4946) // outlineVariant
