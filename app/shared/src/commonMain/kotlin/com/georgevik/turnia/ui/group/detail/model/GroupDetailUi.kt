package com.georgevik.turnia.ui.group.detail.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

sealed interface GroupDetailUi {
    data object Loading : GroupDetailUi

    data class Error(val error: GroupDetailScreenError) : GroupDetailUi

    data class Success(
        val form: GroupForm,
        val eventTypes: List<GroupTypeRowUi>,
        val isNew: Boolean,
        val saving: Boolean = false,
        val isSaved: Boolean = false,
        val userMessage: GroupDetailMessage? = null,
    ) : GroupDetailUi

    @Immutable
    data class GroupForm(
        val groupId: String,
        val name: String,
        val memberCount: Int,
        val invitationCode: String,
        /** Only an admin may change the group's data; everyone else reads it. */
        val editable: Boolean,
    )
}

@Immutable
data class GroupTypeRowUi(
    val typeId: String,
    val groupId: String,
    val name: String,
    val acronym: String?,
    val startTime: String?,
    val endTime: String?,
    val color: Color,
)

enum class GroupDetailScreenError { NotFound, LoadFailed }

enum class GroupDetailMessage { SaveFailed }
