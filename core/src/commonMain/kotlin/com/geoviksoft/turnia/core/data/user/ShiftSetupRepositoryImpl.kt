package com.geoviksoft.turnia.core.data.user

import com.geoviksoft.turnia.core.domain.analytics.Analytics
import com.geoviksoft.turnia.core.domain.analytics.AnalyticsEvent
import com.geoviksoft.turnia.core.domain.model.PersonalEventType
import com.geoviksoft.turnia.core.domain.model.ShiftSetupVia
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.domain.model.UserSession
import com.geoviksoft.turnia.core.domain.repository.AppConfigRepository
import com.geoviksoft.turnia.core.domain.repository.InvitationLinkRepository
import com.geoviksoft.turnia.core.domain.repository.PersonalEventRepository
import com.geoviksoft.turnia.core.domain.repository.ShiftSetupRepository
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.errorOrNull
import com.geoviksoft.turnia.core.system.toFailure
import com.geoviksoft.turnia.core.system.toSuccess
import com.geoviksoft.turnia.core.system.valueOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Decides once per signed-in account whether the setup is owed, reading as little as it can: nothing
 * once the device has settled it, nothing when the cache already shows a type or a group, and only
 * then one bounded read per collection on the server.
 *
 * An empty cache is never taken for an empty account — a fresh install knows nothing yet — so without
 * a server answer the setup is not owed, and the next launch asks again.
 */
class ShiftSetupRepositoryImpl(
    session: StateFlow<UserSession>,
    private val appConfigRepository: AppConfigRepository,
    private val invitations: InvitationLinkRepository,
    private val accountContents: AccountContents,
    private val personalEventRepository: PersonalEventRepository,
    private val analytics: Analytics,
    scope: CoroutineScope,
) : ShiftSetupRepository {

    private val _due = MutableStateFlow(false)
    override val due: StateFlow<Boolean> = _due.asStateFlow()

    private val _hintPending = MutableStateFlow(false)
    override val hintPending: StateFlow<Boolean> = _hintPending.asStateFlow()

    init {
        scope.launch {
            session.map { (it as? UserSession.Authenticated)?.user?.id }
                .distinctUntilChanged()
                .collectLatest { uid ->
                    _due.value = false
                    if (uid != null) _due.value = decide(uid)
                }
        }
        // Someone who arrives through an invitation joins a group that brings its own types.
        scope.launch {
            invitations.pendingCode.filterNotNull().collect {
                if (_due.value) settle()
            }
        }
    }

    private suspend fun decide(uid: UserId): Boolean {
        if (appConfigRepository.isShiftSetupSettled()) return false
        if (invitations.pendingCode.value != null) return false

        if (accountContents.cachedHasAny(uid)) {
            settle()
            return false
        }

        val hasAny = accountContents.serverHasAny(uid).valueOrNull() ?: return false
        if (hasAny) settle()
        return !hasAny
    }

    private suspend fun settle() {
        _due.value = false
        appConfigRepository.setShiftSetupSettled(true)
    }

    override suspend fun shown(via: ShiftSetupVia) {
        analytics.log(AnalyticsEvent.OnboardShiftShown(via))
    }

    override suspend fun skipped(via: ShiftSetupVia, interacted: Boolean) {
        // From the add pane the user only closed a screen they opened: nothing is settled by it.
        if (via == ShiftSetupVia.Onboarding) settle()
        analytics.log(AnalyticsEvent.OnboardShiftSkipped(via, interacted))
    }

    override suspend fun complete(
        types: List<PersonalEventType>,
        via: ShiftSetupVia,
        interacted: Boolean,
        customCount: Int,
    ): Outcome<Unit, Unit> {
        personalEventRepository.createEventTypes(types).errorOrNull()?.let { return it.toFailure() }

        settle()
        analytics.log(
            AnalyticsEvent.OnboardShiftCompleted(
                via = via,
                interacted = interacted,
                typeCount = types.size,
                customTypeCount = customCount,
            )
        )
        // From the add pane the user goes straight back to the day they were adding to.
        if (via == ShiftSetupVia.Onboarding) _hintPending.value = true
        return Unit.toSuccess()
    }

    override fun hintShown() {
        _hintPending.value = false
    }
}
