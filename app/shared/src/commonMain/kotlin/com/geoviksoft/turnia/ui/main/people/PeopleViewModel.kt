package com.geoviksoft.turnia.ui.main.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.domain.model.UserProfile
import com.geoviksoft.turnia.core.domain.repository.UserRepository
import com.geoviksoft.turnia.core.domain.username.USERNAME_SEARCH_MIN_LENGTH
import com.geoviksoft.turnia.core.system.fold
import com.geoviksoft.turnia.core.system.onFailure
import com.geoviksoft.turnia.core.system.valueOrEmpty
import com.geoviksoft.turnia.ui.main.people.model.PeopleFilter
import com.geoviksoft.turnia.ui.main.people.model.PersonRowUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class PeopleViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val filterSelected = MutableStateFlow(PeopleFilter.SHARED_WITH_ME)

    private val sharedByMe = MutableStateFlow<List<PersonRowUi>?>(null)

    private val search = MutableStateFlow(SearchUi())
    private val searchQuery = MutableStateFlow("")
    private val userMessage = MutableStateFlow<PeopleMessage?>(null)

    /** Hiding filters the one query rather than narrowing it: a `not-in` would re-bill it on every change. */
    private val sharedWithMe = combine(
        userRepository.getCalendarsSharedWithMe()
            .onEach { outcome ->
                outcome.onFailure { userMessage.value = PeopleMessage.SharedWithMeLoadFailed }
            }
            .map { outcome -> outcome.valueOrEmpty() },
        userRepository.getHiddenSharedCalendars(),
    ) { profiles, hiddenUserIds ->
        val (hidden, visible) = profiles.partition { it.id in hiddenUserIds }
        SharedWithMe(visible = visible.map(::toRow), hidden = hidden.map(::toRow))
    }

    val uiState: StateFlow<PeopleUi> =
        combine(
            filterSelected,
            sharedByMe,
            sharedWithMe,
            search,
            userMessage,
        ) { selected, byMe, withMe, search, message ->
            if (byMe == null) {
                PeopleUi.Loading
            } else {
                PeopleUi.Success(
                    filter = shownFilter(selected, withMe),
                    sharedByMe = byMe,
                    sharedWithMe = withMe.visible,
                    hidden = withMe.hidden,
                    search = search,
                    userMessage = message,
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT),
            initialValue = PeopleUi.Loading,
        )

    init {
        viewModelScope.launch {
            searchQuery.debounce(SEARCH_DEBOUNCE).collectLatest(::runSearch)
        }
        refresh()
    }

    fun filterSelected(selected: PeopleFilter) {
        filterSelected.value = selected
    }

    fun refresh() {
        viewModelScope.launch {
            userRepository.getCalendarSharedWith().fold(
                onSuccess = { users -> sharedByMe.value = users.map(::toRow) },
                onFailure = {
                    sharedByMe.value = emptyList()
                    userMessage.value = PeopleMessage.SharedByMeLoadFailed
                },
            )
        }
    }

    fun onSearchChanged(query: String) {
        val tooShort = query.trim().length < USERNAME_SEARCH_MIN_LENGTH

        search.update {
            it.copy(query = query, panel = if (tooShort) SearchUi.Panel.TooShort else it.panel)
        }
        searchQuery.value = query
    }

    fun onSearchDismissed() {
        search.value = SearchUi()
        searchQuery.value = ""
    }

    fun onGrant(userId: UserId) {
        viewModelScope.launch {
            userRepository.grantCalendarAccess(userId).fold(
                onSuccess = {
                    onSearchDismissed()
                    refresh()
                },
                onFailure = { userMessage.value = PeopleMessage.GrantFailed },
            )
        }
    }

    fun onRevoke(userId: UserId) {
        viewModelScope.launch {
            userRepository.revokeCalendarAccess(userId).fold(
                onSuccess = { refresh() },
                onFailure = { userMessage.value = PeopleMessage.RevokeFailed },
            )
        }
    }

    fun onHide(userId: UserId) {
        filterSelected.value = PeopleFilter.SHARED_WITH_ME
        userMessage.value = PeopleMessage.Hidden(userId)

        viewModelScope.launch {
            userRepository.hideSharedCalendar(userId)
                .onFailure { userMessage.value = PeopleMessage.HideFailed }
        }
    }

    fun onUnhide(userId: UserId) {
        val hidden = (uiState.value as? PeopleUi.Success)?.hidden.orEmpty()
        if (hidden.singleOrNull()?.id == userId) filterSelected.value = PeopleFilter.SHARED_WITH_ME
        userMessage.value = PeopleMessage.Unhidden(userId)

        viewModelScope.launch {
            userRepository.unhideSharedCalendar(userId)
                .onFailure { userMessage.value = PeopleMessage.UnhideFailed }
        }
    }

    fun userMessageShown(shown: PeopleMessage, undo: Boolean = false) {
        userMessage.compareAndSet(shown, null)
        if (!undo) return

        viewModelScope.launch {
            when (shown) {
                is PeopleMessage.Hidden -> userRepository.unhideSharedCalendar(shown.userId)
                    .onFailure { userMessage.value = PeopleMessage.UnhideFailed }

                is PeopleMessage.Unhidden -> userRepository.hideSharedCalendar(shown.userId)
                    .onFailure { userMessage.value = PeopleMessage.HideFailed }

                else -> Unit
            }
        }
    }

    /** The hidden chip is not drawn without rows, so neither can it be the one on screen. */
    private fun shownFilter(selected: PeopleFilter, withMe: SharedWithMe): PeopleFilter =
        if (selected == PeopleFilter.HIDDEN && withMe.hidden.isEmpty()) PeopleFilter.SHARED_WITH_ME
        else selected

    private suspend fun runSearch(query: String) {
        val prefix = query.trim()
        if (prefix.length < USERNAME_SEARCH_MIN_LENGTH) {
            updatePanel(SearchUi.Panel.TooShort)
            return
        }

        updateSearchLoading()

        val shared = sharedByMe.value.orEmpty().mapTo(mutableSetOf()) { it.id }
        val results = userRepository.searchUsers(prefix).valueOrEmpty()
            // Sharing with yourself is not a thing, and the row would be confusing.
            .filterNot { it.id == userRepository.loggedUser?.id }
            .map { user ->
                SearchResultUi(
                    id = user.id,
                    name = user.name,
                    username = user.username,
                    alreadyShared = user.id in shared,
                    avatar = user.avatar,
                )
            }

        updatePanel(
            if (results.isEmpty()) SearchUi.Panel.Empty else SearchUi.Panel.Results(users = results, isLoading = false)
        )
    }

    private fun updatePanel(panel: SearchUi.Panel) = search.update { it.copy(panel = panel) }
    private fun updateSearchLoading() {
        search.update {
            if (it.panel is SearchUi.Panel.Results) {
                it.copy(panel = it.panel.copy(isLoading = true))
            } else {

                it.copy(panel = SearchUi.Panel.Searching)
            }
        }
    }


    private fun toRow(user: UserProfile) = PersonRowUi(
        id = user.id,
        name = user.name,
        username = user.username,
        avatar = user.avatar,
    )

    private data class SharedWithMe(val visible: List<PersonRowUi>, val hidden: List<PersonRowUi>)

    private companion object {
        const val SUBSCRIPTION_TIMEOUT = 5_000L
        val SEARCH_DEBOUNCE = 300.milliseconds
    }
}
