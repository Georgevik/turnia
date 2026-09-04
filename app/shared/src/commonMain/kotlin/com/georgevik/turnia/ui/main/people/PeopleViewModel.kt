package com.georgevik.turnia.ui.main.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.domain.repository.UserRepository
import com.georgevik.turnia.core.system.fold
import com.georgevik.turnia.ui.main.people.model.ColleagueRowUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PeopleViewModel(private val userRepository: UserRepository) : ViewModel() {

    // The unfiltered list: the search narrows a copy of it, never the state it just produced.
    private var allColleagues: List<ColleagueRowUi> = emptyList()

    private val _uiState = MutableStateFlow<PeopleUi>(PeopleUi.Loading)
    val uiState: StateFlow<PeopleUi> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            userRepository.getCalendarsSharedWithMe().fold(
                onSuccess = { profiles ->
                    allColleagues = profiles.map {
                        ColleagueRowUi(id = it.id, name = it.name, username = it.username)
                    }
                    _uiState.value = PeopleUi.Success(colleagues = allColleagues)
                },
                onFailure = {
                    _uiState.value = PeopleUi.Success(userMessage = PeopleMessage.LoadFailed)
                },
            )
        }
    }

    fun searchBy(query: String) = updateSuccess { state ->
        state.copy(
            query = query,
            colleagues = allColleagues.filter { it.matches(query) },
        )
    }

    fun userMessageShown() = updateSuccess { it.copy(userMessage = null) }

    private fun ColleagueRowUi.matches(query: String) =
        name.contains(query, ignoreCase = true) || username.contains(query, ignoreCase = true)

    private fun updateSuccess(block: (PeopleUi.Success) -> PeopleUi.Success) =
        _uiState.update { state -> if (state is PeopleUi.Success) block(state) else state }
}
