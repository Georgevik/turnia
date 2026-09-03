package com.georgevik.turnia.ui.main.sharecalendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.domain.model.UserProfile
import com.georgevik.turnia.core.domain.repository.UserRepository
import com.georgevik.turnia.core.domain.username.USERNAME_SEARCH_MIN_LENGTH
import com.georgevik.turnia.core.system.fold
import com.georgevik.turnia.core.system.valueOrEmpty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class ShareCalendarViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<ShareCalendarUi>(ShareCalendarUi.Loading)
    val uiState: StateFlow<ShareCalendarUi> = _uiState.asStateFlow()

    private val _searchFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _searchFlow.debounce(SEARCH_DEBOUNCE).collectLatest { query -> search(query) }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = ShareCalendarUi.Loading

            userRepository.getCalendarSharedWith().fold(
                onSuccess = { users ->
                    _uiState.value = ShareCalendarUi.Success(sharedWith = users.map(::toRow))
                },
                onFailure = {
                    _uiState.value = ShareCalendarUi.Success(
                        userMessage = ShareCalendarMessage.LoadFailed
                    )
                },
            )
        }
    }

    fun onSearchChanged(query: String) {
        // Shortening the query drops any results on screen straight away, without waiting for the
        // debounce: they no longer match what is typed.
        val panel = SearchUi.Panel.TooShort.takeIf { query.trim().length < USERNAME_SEARCH_MIN_LENGTH }

        updateSuccess {
            it.copy(search = it.search.copy(query = query, panel = panel ?: it.search.panel))
        }
        viewModelScope.launch { _searchFlow.emit(query) }
    }

    fun onSearchDismissed() {
        updateSuccess { it.copy(search = SearchUi()) }
        viewModelScope.launch { _searchFlow.emit("") }
    }

    fun onGrant(userId: String) {
        viewModelScope.launch {
            userRepository.grantCalendarAccess(userId).fold(
                onSuccess = {
                    onSearchDismissed()
                    refresh()
                },
                onFailure = { showMessage(ShareCalendarMessage.GrantFailed) },
            )
        }
    }

    fun onRevoke(userId: String) {
        viewModelScope.launch {
            userRepository.revokeCalendarAccess(userId).fold(
                onSuccess = { refresh() },
                onFailure = { showMessage(ShareCalendarMessage.RevokeFailed) },
            )
        }
    }

    fun userMessageShown() = updateSuccess { it.copy(userMessage = null) }

    private suspend fun search(query: String) {
        val prefix = query.trim()
        if (prefix.length < USERNAME_SEARCH_MIN_LENGTH) {
            updateSearch { SearchUi.Panel.TooShort }
            return
        }

        updateSearch { SearchUi.Panel.Searching }

        val current = _uiState.value as? ShareCalendarUi.Success ?: return
        val shared = current.sharedWith.map { it.id }.toSet()
        val results = userRepository.searchUsers(prefix).valueOrEmpty()
            // Sharing with yourself is not a thing, and the row would be confusing.
            .filterNot { it.id == userRepository.loggedUser?.firebaseUid }
            .map { user ->
                SearchResultUi(
                    id = user.id,
                    name = user.name,
                    username = user.username,
                    alreadyShared = user.id in shared,
                )
            }

        updateSearch {
            if (results.isEmpty()) SearchUi.Panel.Empty else SearchUi.Panel.Results(results)
        }
    }

    private fun updateSearch(panel: () -> SearchUi.Panel) =
        updateSuccess { it.copy(search = it.search.copy(panel = panel())) }

    private fun showMessage(message: ShareCalendarMessage) =
        updateSuccess { it.copy(userMessage = message) }

    private fun updateSuccess(block: (ShareCalendarUi.Success) -> ShareCalendarUi.Success) =
        _uiState.update { state ->
            if (state is ShareCalendarUi.Success) block(state) else state
        }

    private fun toRow(user: UserProfile) =
        SharedUserUi(id = user.id, name = user.name, username = user.username)

    companion object {
        private val SEARCH_DEBOUNCE = 300.milliseconds
    }
}
