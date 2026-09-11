package com.geoviksoft.turnia.core.data.invitation

import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.domain.model.InvitationLink
import com.geoviksoft.turnia.core.domain.repository.InvitationLinkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InvitationLinkRepositoryImpl : InvitationLinkRepository {

    private val _pendingCode = MutableStateFlow<String?>(null)
    override val pendingCode: StateFlow<String?> = _pendingCode.asStateFlow()

    override fun opened(link: String) {
        val code = InvitationLink.codeOf(link) ?: return
        Logger.i(TAG, "Invitation link opened")

        _pendingCode.value = code
    }

    override fun referred(code: String) {
        val valid = InvitationLink.codeOrNull(code) ?: return
        Logger.i(TAG, "Installed from an invitation link")

        _pendingCode.value = valid
    }

    override fun codeHandled() {
        _pendingCode.value = null
    }

    private companion object {
        const val TAG = "InvitationLinkRepository"
    }
}
