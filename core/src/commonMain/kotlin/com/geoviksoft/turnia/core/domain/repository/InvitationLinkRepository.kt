package com.geoviksoft.turnia.core.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * The invitation code an opened link brought in, waiting for the join sheet to take it.
 *
 * State and not an event stream, for the same reason as [NotificationRepository]: the platform
 * delivers a link whenever it likes — on a cold start, or before the user has even signed in — so
 * the code is held until the Groups tab says it has put it in the sheet.
 */
interface InvitationLinkRepository {

    val pendingCode: StateFlow<String?>

    /** The user opened [link]. Anything that is not an invitation link is ignored. */
    fun opened(link: String)

    /** The app was installed from an invitation link, and the store handed its code over. */
    fun referred(code: String)

    /** Called by the UI once the code is in the join sheet, so it is not offered twice. */
    fun codeHandled()

    /**
     * The user asked to join with a code they have yet to type — "I have a code" on the team
     * prompt. Kept apart from [pendingCode], which means an invitation is waiting and holds the
     * shift setup back.
     */
    val joinSheetRequested: StateFlow<Boolean>

    fun requestJoinSheet()

    /** Called by the Groups tab once the join sheet is open. */
    fun joinSheetOpened()
}
