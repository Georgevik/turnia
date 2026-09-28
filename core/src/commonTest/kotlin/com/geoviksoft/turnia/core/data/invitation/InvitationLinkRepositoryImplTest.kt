package com.geoviksoft.turnia.core.data.invitation

import com.geoviksoft.turnia.core.fakes.RecordingAnalytics
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InvitationLinkRepositoryImplTest {

    private val repository = InvitationLinkRepositoryImpl(RecordingAnalytics())

    @Test
    fun aRequestedJoinSheetWaitsUntilItIsOpened() {
        repository.requestJoinSheet()
        assertTrue(repository.joinSheetRequested.value)

        repository.joinSheetOpened()
        assertFalse(repository.joinSheetRequested.value)
    }

    @Test
    fun aRequestedJoinSheetIsNotAnInvitation() {
        repository.requestJoinSheet()

        assertNull(repository.pendingCode.value)
    }
}
