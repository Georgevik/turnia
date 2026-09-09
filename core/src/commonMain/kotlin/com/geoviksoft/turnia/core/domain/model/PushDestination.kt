package com.geoviksoft.turnia.core.domain.model

/**
 * Where tapping a notification should leave the user.
 *
 * Named after the destination and not the notification, because several kinds of message can
 * reasonably lead to the same place, and because the UI owns which screen that turns out to be.
 */
sealed interface PushDestination {
    /** A group of the user's, opened on the screen that lists who is waiting to be let in. */
    data class GroupDetail(val groupId: GroupId) : PushDestination

    data object Groups : PushDestination

    data object People : PushDestination

    /**
     * The swap tab. Both swap notifications land here rather than on the shift they are about: what
     * the reader wants next is a list they can act on — the offer to accept, or the shift somebody
     * has just taken off them — and each is a tab away. Carrying the event id would only buy a
     * scroll position.
     */
    data object Swap : PushDestination
}
