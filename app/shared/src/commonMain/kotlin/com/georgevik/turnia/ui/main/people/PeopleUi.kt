package com.georgevik.turnia.ui.main.people

import com.georgevik.turnia.ui.main.people.model.ColleagueRowUi

sealed interface PeopleUi {

    data object Loading : PeopleUi

    data class Success(
        val colleagues: List<ColleagueRowUi> = emptyList(),
        val userMessage: PeopleMessage? = null,
    ) : PeopleUi
}

enum class PeopleMessage { LoadFailed }
