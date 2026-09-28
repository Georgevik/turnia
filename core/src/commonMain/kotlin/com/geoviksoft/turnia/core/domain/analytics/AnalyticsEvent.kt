package com.geoviksoft.turnia.core.domain.analytics

import com.geoviksoft.turnia.core.domain.model.EventKind
import com.geoviksoft.turnia.core.domain.model.SharePrompt
import com.geoviksoft.turnia.core.domain.model.ShiftSetupVia

/**
 * Every event name and parameter key the app reports. They are declared here rather than written at
 * each call site because the console groups by the literal string: renaming one starts a fresh
 * series and leaves the history behind it orphaned, with no way to stitch the two back together.
 */
sealed class AnalyticsEvent(
    val name: String,
    val parameters: Map<String, Any> = emptyMap(),
) {
    /** `screen_view` and `screen_name` are Google's own names; the console reads no others. */
    class ScreenView(screen: String) : AnalyticsEvent(
        name = "screen_view",
        parameters = mapOf("screen_name" to screen),
    )

    class GroupCreated(autoApprove: Boolean, typeCount: Int) : AnalyticsEvent(
        name = "group_created",
        parameters = mapOf("auto_approve" to autoApprove.value(), "type_count" to typeCount.toLong()),
    )

    /** Google's recommended name, so the console reports it as a sign-up out of the box. */
    class SignUp(method: String) : AnalyticsEvent(
        name = "sign_up",
        parameters = mapOf("method" to method),
    )

    class SharePromptShown(prompt: SharePrompt) :
        AnalyticsEvent("share_prompt_shown", prompt.parameters())

    class SharePromptShared(prompt: SharePrompt) :
        AnalyticsEvent("share_prompt_shared", prompt.parameters())

    class SharePromptDismissed(prompt: SharePrompt) :
        AnalyticsEvent("share_prompt_dismissed", prompt.parameters())

    data object GroupEventCreated : AnalyticsEvent("group_event_created")

    data object JoinRequested : AnalyticsEvent("join_group_requested")

    /** Google's own `join_group`: the request was accepted and the user is now a member. */
    data object JoinAccepted : AnalyticsEvent("join_group")

    /** Google's recommended name. A new account logs [SignUp] instead, never both. */
    class Login(method: String) : AnalyticsEvent(
        name = "login",
        parameters = mapOf("method" to method),
    )

    data object PersonalEventCreated : AnalyticsEvent("personal_event_created")

    data object PersonalEventDeleted : AnalyticsEvent("personal_event_deleted")

    data object EventNotesSaved : AnalyticsEvent("event_notes_saved")

    class OneOffEventCreated(allDay: Boolean, multiMonth: Boolean) : AnalyticsEvent(
        name = "one_off_event_created",
        parameters = mapOf("all_day" to allDay.value(), "multi_month" to multiMonth.value()),
    )

    data object OneOffEventUpdated : AnalyticsEvent("one_off_event_updated")

    data object OneOffEventDeleted : AnalyticsEvent("one_off_event_deleted")

    data object GroupEventDeleted : AnalyticsEvent("group_event_deleted")

    data object PersonalEventTypeCreated : AnalyticsEvent("personal_event_type_created")

    data object GroupEventTypeCreated : AnalyticsEvent("group_event_type_created")

    data object SwapOffered : AnalyticsEvent("swap_offered")

    data object SwapWithdrawn : AnalyticsEvent("swap_withdrawn")

    data object SwapTaken : AnalyticsEvent("swap_taken")

    data object SwapReturned : AnalyticsEvent("swap_returned")

    /** Somebody else took the shift first: the race is only worth guarding if this ever shows. */
    data object SwapTakeLost : AnalyticsEvent("swap_take_lost")

    /** The share sheet opened; the platform never says whether anything was actually sent. */
    data object GroupInviteShared : AnalyticsEvent("group_invite_shared")

    class InvitationOpened(via: InvitationSource) : AnalyticsEvent(
        name = "invitation_opened",
        parameters = mapOf("via" to via.value),
    )

    data object JoinRequestRejected : AnalyticsEvent("join_request_rejected")

    data object CalendarShared : AnalyticsEvent("calendar_shared")

    data object CalendarShareRevoked : AnalyticsEvent("calendar_share_revoked")

    data object SharedCalendarViewed : AnalyticsEvent("shared_calendar_viewed")

    data object SharedCalendarHidden : AnalyticsEvent("shared_calendar_hidden")

    data object GroupLeft : AnalyticsEvent("group_left")

    data object MemberRemoved : AnalyticsEvent("member_removed")

    data object GroupDeleted : AnalyticsEvent("group_deleted")

    data object AccountDeleted : AnalyticsEvent("account_deleted")

    class OnboardShiftShown(via: ShiftSetupVia) : AnalyticsEvent(
        name = "onboard_shift_shown",
        parameters = mapOf("via" to via.value),
    )

    /** [interacted]: the user touched the panel before leaving it, rather than skipping it blind. */
    class OnboardShiftSkipped(via: ShiftSetupVia, interacted: Boolean) : AnalyticsEvent(
        name = "onboard_shift_skipped",
        parameters = mapOf("via" to via.value, "interacted" to interacted.value()),
    )

    class OnboardShiftCompleted(
        via: ShiftSetupVia,
        interacted: Boolean,
        typeCount: Int,
        customTypeCount: Int,
    ) : AnalyticsEvent(
        name = "onboard_shift_completed",
        parameters = mapOf(
            "via" to via.value,
            "interacted" to interacted.value(),
            "type_count" to typeCount.toLong(),
            "custom_type_count" to customTypeCount.toLong(),
        ),
    )

    /** Activation: whether the first event added on the device came from a shift or was a one-off. */
    class FirstEventAdded(kind: EventKind) : AnalyticsEvent(
        name = "first_event_added",
        parameters = mapOf("kind" to kind.value()),
    )

    /** [type] is the push's own routing `type`, only ever one the app recognises. */
    class NotificationOpened(type: String) : AnalyticsEvent(
        name = "notification_opened",
        parameters = mapOf("type" to type),
    )
}

/** How an invitation code reached the app. [value] is what analytics carries. */
enum class InvitationSource(val value: String) {
    Link("link"),

    /** Android only: Play hands the code over on the first launch after an install. */
    InstallReferrer("install_referrer"),
}

private fun EventKind.value(): String = when (this) {
    EventKind.Typed -> "typed"
    EventKind.OneOff -> "one_off"
}

/** Analytics takes strings and numbers only; a boolean is reported as its name. */
internal fun Boolean.value(): String = toString()

private fun SharePrompt.parameters(): Map<String, Any> =
    mapOf("audience" to audience.value, "milestone" to milestone.toLong())
