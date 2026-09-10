package com.geoviksoft.turnia.ui.system.avatar

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.allDrawableResources

private const val ANIMAL_ICON_PREFIX = "animal_icon_"

/**
 * The animals a user can pick from, by the id stored on their profile.
 *
 * Read off the generated resource table rather than listed by hand: dropping a new
 * `animal_icon_*.png` into `composeResources` is then the whole change. Sorted because the table is
 * a map and its iteration order is not guaranteed — an unsorted grid would reshuffle itself.
 */
@OptIn(ExperimentalResourceApi::class)
val AnimalIconIds: List<String> = Res.allDrawableResources.keys
    .filter { it.startsWith(ANIMAL_ICON_PREFIX) }
    .map { it.removePrefix(ANIMAL_ICON_PREFIX) }
    .sorted()

/** Null when the user has picked no animal, or picked one this build no longer ships. */
@OptIn(ExperimentalResourceApi::class)
fun animalIcon(id: String?): DrawableResource? =
    id?.let { Res.allDrawableResources[ANIMAL_ICON_PREFIX + it] }
