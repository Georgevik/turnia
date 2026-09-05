package com.georgevik.turnia.ui.main.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.domain.repository.UserRepository
import com.georgevik.turnia.core.system.onFailure
import com.georgevik.turnia.core.system.valueOrEmpty
import com.georgevik.turnia.ui.main.people.model.ColleagueRowUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

class PeopleViewModel(userRepository: UserRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    private val userMessage = MutableStateFlow<PeopleMessage?>(null)
    private val colleagues = userRepository.getCalendarsSharedWithMe()
        .onEach { outcome -> outcome.onFailure { userMessage.value = PeopleMessage.LoadFailed } }
        .map { outcome ->
            outcome.valueOrEmpty().map { ColleagueRowUi(it.id, it.name, it.username) }
        }

    val uiState: StateFlow<PeopleUi> =
        combine(query, userMessage, colleagues) { query, message, colleagues ->
            PeopleUi.Success(
                query = query,
                colleagues = colleagues.filter { it.matches(query) },
                userMessage = message,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT),
            initialValue = PeopleUi.Loading,
        )

    fun searchBy(value: String) {
        query.value = value
    }

    fun userMessageShown() {
        userMessage.value = null
    }

    private fun ColleagueRowUi.matches(query: String) =
        name.contains(query, ignoreCase = true) || username.contains(query, ignoreCase = true)

    private companion object {
        const val SUBSCRIPTION_TIMEOUT = 5_000L
    }
}
