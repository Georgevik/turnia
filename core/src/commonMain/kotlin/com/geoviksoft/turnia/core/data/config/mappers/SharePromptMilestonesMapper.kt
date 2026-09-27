package com.geoviksoft.turnia.core.data.config.mappers

import com.geoviksoft.turnia.core.system.outcomeCatching
import com.geoviksoft.turnia.core.system.valueOrNull
import kotlinx.serialization.json.Json

/**
 * `sharePromptMilestones` as Remote Config stores it: it has no array type, so the milestones travel
 * as a JSON array in a string. A value that does not parse means no milestones — the prompt stays
 * away rather than the app crashing.
 */
class SharePromptMilestonesMapper {

    fun map(raw: String): List<Int> =
        outcomeCatching(TAG, mapError = {}) { Json.decodeFromString<List<Int>>(raw) }
            .valueOrNull()
            .orEmpty()
            .filter { it > 0 }
            .distinct()
            .sorted()

    private companion object {
        const val TAG = "SharePromptMilestonesMapper"
    }
}
