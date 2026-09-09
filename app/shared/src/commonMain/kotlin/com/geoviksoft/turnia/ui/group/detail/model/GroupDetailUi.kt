package com.geoviksoft.turnia.ui.group.detail.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.geoviksoft.turnia.core.domain.model.EventTypeId
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.domain.model.UserProfile

sealed interface GroupDetailUi {
    data object Loading : GroupDetailUi

    data class Error(val error: GroupDetailScreenError) : GroupDetailUi

    data class Success(
        val form: GroupForm,
        val eventTypes: List<GroupTypeRowUi>,
        val members: List<GroupMemberUi>,
        /** Empty for anyone but an admin: the rules do not let a member read them. */
        val joinRequests: List<JoinRequestUi>,
        val isNew: Boolean,
        val saving: Boolean = false,
        val isSaved: Boolean = false,
        val hasLeft: Boolean = false,
        val close: GroupCloseUi? = null,
        val userMessage: GroupDetailMessage? = null,
    ) : GroupDetailUi

    @Immutable
    data class GroupForm(
        /** Null while the group is being created: it has no id until it is saved. */
        val groupId: GroupId?,
        val name: String,
        /** The group's accent, the same for every member: only an admin changes it. */
        val color: Color,
        val memberCount: Int,
        /** Null when the admin keeps the code to themselves. */
        val invitationCode: String?,
        val autoApprove: Boolean,
        val membersCanSeeCode: Boolean,
        /** A regenerated code only reaches the group when the form is saved. */
        val codeChanged: Boolean = false,
        /** Only an admin may change the group's data; everyone else reads it. */
        val editable: Boolean,
    )
}

@Immutable
data class GroupTypeRowUi(
    val typeId: EventTypeId,
    /** Null while the group is being created: the type is a draft with no group to belong to. */
    val groupId: GroupId?,
    val name: String,
    val acronym: String?,
    val startTime: String?,
    val endTime: String?,
    val color: Color,
)

@Immutable
data class GroupMemberUi(
    val id: UserId,
    val name: String,
    val username: String,
    val isAdmin: Boolean,
    val avatar: UserProfile.AnimalAvatar = UserProfile.AnimalAvatar.NONE,
)

@Immutable
data class JoinRequestUi(
    val userId: UserId,
    val name: String,
    val username: String,
    val avatar: UserProfile.AnimalAvatar = UserProfile.AnimalAvatar.NONE,
)

/**
 * Leaving or deleting the group. The confirmation dialog stays up for the whole thing and reports
 * the outcome itself, so the state survives until the UI says it has been shown.
 */
sealed interface GroupCloseUi {
    data object Running : GroupCloseUi

    data object Succeeded : GroupCloseUi

    data class Failed(val message: GroupDetailMessage) : GroupCloseUi
}

enum class GroupDetailScreenError { NotFound, LoadFailed }

enum class GroupDetailMessage {
    SaveFailed,
    RequestFailed,
    RemoveMemberFailed,
    LeaveFailed,
    LeaveLastAdmin,
    DeleteFailed,
    DeleteNotEmpty,
}
