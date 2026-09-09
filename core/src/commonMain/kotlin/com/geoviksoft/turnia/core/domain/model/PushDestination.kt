package com.geoviksoft.turnia.core.domain.model

/**
 * Where tapping a notification should leave the user.
 *
 * Named after the destination and not the notification, because several kinds of message can
 * reasonably lead to the same place, and because the UI owns which screen that turns out to be.
 * A push with nothing worth navigating to — a shift put up for swap, for now — produces none.
 */
sealed interface PushDestination {
    /** A group of the user's, opened on the screen that lists who is waiting to be let in. */
    data class GroupDetail(val groupId: GroupId) : PushDestination

    data object Groups : PushDestination

    data object People : PushDestination
}
