package com.geoviksoft.turnia.core.data.invitation

import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.domain.analytics.Analytics
import com.geoviksoft.turnia.core.domain.analytics.AnalyticsEvent
import com.geoviksoft.turnia.core.domain.analytics.InvitationSource
import com.geoviksoft.turnia.core.domain.model.InvitationLink
import com.geoviksoft.turnia.core.domain.repository.InvitationLinkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InvitationLinkRepositoryImpl(private val analytics: Analytics) : InvitationLinkRepository {

    private val _pendingCode = MutableStateFlow<String?>(null)
    override val pendingCode: StateFlow<String?> = _pendingCode.asStateFlow()

    override fun opened(link: String) {
        val code = InvitationLink.codeOf(link) ?: return
        Logger.i(TAG, "Invitation link opened")
        analytics.log(AnalyticsEvent.InvitationOpened(InvitationSource.Link))

        _pendingCode.value = code
    }

    override fun referred(code: String) {
        val valid = InvitationLink.codeOrNull(code) ?: return
        Logger.i(TAG, "Installed from an invitation link")
        analytics.log(AnalyticsEvent.InvitationOpened(InvitationSource.InstallReferrer))

        _pendingCode.value = valid
    }

    override fun codeHandled() {
        _pendingCode.value = null
    }

    private val _joinSheetRequested = MutableStateFlow(false)
    override val joinSheetRequested: StateFlow<Boolean> = _joinSheetRequested.asStateFlow()

    override fun requestJoinSheet() {
        _joinSheetRequested.value = true
    }

    override fun joinSheetOpened() {
        _joinSheetRequested.value = false
    }

    private companion object {
        const val TAG = "InvitationLinkRepository"
    }
}
