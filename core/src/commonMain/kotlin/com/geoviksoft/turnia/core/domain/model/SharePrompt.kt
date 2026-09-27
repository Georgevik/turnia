package com.geoviksoft.turnia.core.domain.model

import com.geoviksoft.turnia.core.system.outcomeCatching
import com.geoviksoft.turnia.core.system.valueOrNull
import kotlinx.serialization.json.Json

/** Who the share prompt speaks to. [value] is what analytics and the link's campaign carry. */
enum class SharePromptAudience(val value: String) {
    /** The event that reached the milestone had a type: a shift, which is a team's business. */
    Coworkers("coworkers"),

    /** It was a one-off: a plan, which is what friends want to know about. */
    Friends("friends"),
}

/** The kind of event the user just added, which decides the prompt's [SharePromptAudience]. */
enum class EventKind(val audience: SharePromptAudience) {
    Typed(SharePromptAudience.Coworkers),
    OneOff(SharePromptAudience.Friends),
}

/** A prompt waiting to be shown for [milestone] events added. */
data class SharePrompt(
    val audience: SharePromptAudience,
    val milestone: Int,
)

enum class SharePromptAnswer { Shared, Dismissed }

object SharePromptMilestones {

    /**
     * Remote Config has no array type, so the milestones travel as a JSON array in a string. A value
     * that does not parse means no milestones: the prompt stays away rather than the app crashing.
     */
    fun parse(raw: String): List<Int> =
        outcomeCatching(TAG, mapError = {}) { Json.decodeFromString<List<Int>>(raw) }
            .valueOrNull()
            .orEmpty()
            .filter { it > 0 }
            .distinct()
            .sorted()

    private const val TAG = "SharePromptMilestones"
}
