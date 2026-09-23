package com.geoviksoft.turnia.ui.main.people

import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.domain.model.UserProfile
import com.geoviksoft.turnia.ui.main.people.model.PeopleFilter
import com.geoviksoft.turnia.ui.main.people.model.PersonRowUi

sealed interface PeopleUi {

    data object Loading : PeopleUi

    data class Success(
        val filter: PeopleFilter,
        val sharedByMe: List<PersonRowUi> = emptyList(),
        val sharedWithMe: List<PersonRowUi> = emptyList(),
        val hidden: List<PersonRowUi> = emptyList(),
        val search: SearchUi = SearchUi(),
        val userMessage: PeopleMessage? = null,
    ) : PeopleUi
}

data class SearchUi(
    val query: String = "",
    val panel: Panel = Panel.TooShort,
) {
    sealed interface Panel {
        data object TooShort : Panel
        data object Searching : Panel
        data object Empty : Panel
        data class Results(val users: List<SearchResultUi>, val isLoading: Boolean) : Panel
    }
}

data class SearchResultUi(
    val id: UserId,
    val name: String,
    val username: String,
    val alreadyShared: Boolean,
    val avatar: UserProfile.AnimalAvatar = UserProfile.AnimalAvatar.NONE,
)

sealed interface PeopleMessage {
    data object SharedWithMeLoadFailed : PeopleMessage
    data object SharedByMeLoadFailed : PeopleMessage
    data object GrantFailed : PeopleMessage
    data object RevokeFailed : PeopleMessage
    data object HideFailed : PeopleMessage
    data object UnhideFailed : PeopleMessage

    /** Offers to undo, which is why it names whose calendar it was. */
    data class Hidden(val userId: UserId) : PeopleMessage
    data class Unhidden(val userId: UserId) : PeopleMessage
}
