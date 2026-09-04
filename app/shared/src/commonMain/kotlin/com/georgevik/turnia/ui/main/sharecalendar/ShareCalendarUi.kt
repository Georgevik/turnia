package com.georgevik.turnia.ui.main.sharecalendar

import com.georgevik.turnia.core.domain.model.UserId

sealed interface ShareCalendarUi {

    data object Loading : ShareCalendarUi

    /**
     * A list that failed to load is still this state, empty and carrying [userMessage]: the screen
     * remains usable — you can share with someone — so the failure is a message, not a dead end.
     */
    data class Success(
        val sharedWith: List<SharedUserUi> = emptyList(),
        val search: SearchUi = SearchUi(),
        val userMessage: ShareCalendarMessage? = null,
    ) : ShareCalendarUi
}

/**
 * Someone this user granted read access to their calendar. [name] and [username] are blank when
 * that user has no username reservation to resolve them from.
 */
data class SharedUserUi(
    val id: UserId,
    val name: String,
    val username: String,
)

data class SearchUi(
    val query: String = "",
    val panel: Panel = Panel.TooShort,
) {
    /**
     * Which of the four things the search area shows. Deciding it here rather than re-deriving it
     * from a query length and a couple of booleans keeps the states that cannot coexist —
     * searching *and* holding results, empty *and* too short — from being representable.
     */
    sealed interface Panel {
        /** The query is too short to run: say so instead of claiming nobody matched. */
        data object TooShort : Panel
        data object Searching : Panel
        data object Empty : Panel
        data class Results(val users: List<SearchResultUi>) : Panel
    }
}

/** [alreadyShared] rows stay visible but cannot be picked, so a search never looks empty-handed. */
data class SearchResultUi(
    val id: UserId,
    val name: String,
    val username: String,
    val alreadyShared: Boolean,
)

enum class ShareCalendarMessage { LoadFailed, GrantFailed, RevokeFailed }