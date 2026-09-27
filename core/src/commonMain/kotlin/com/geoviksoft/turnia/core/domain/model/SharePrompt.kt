package com.geoviksoft.turnia.core.domain.model

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
