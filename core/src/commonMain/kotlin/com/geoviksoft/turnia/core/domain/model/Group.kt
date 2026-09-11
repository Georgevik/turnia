package com.geoviksoft.turnia.core.domain.model

data class Group(
    val id: GroupId,
    val name: String,
    val types: List<GroupEventType>,
    val members: List<GroupMember>,
    val memberCount: Int,
    /** Every member can invite with it, through the link; only an admin sees it or changes it. */
    val invitationCode: String,
    /** Whoever knows the code walks in; otherwise an admin has to accept the request. */
    val autoApprove: Boolean,
    val isAdmin: Boolean,
    val color: String? = null,
    /**
     * The user left this group, or an admin removed them, and they still hold events in it. They
     * see only their own, only the types those use, and can add nothing more.
     */
    val isRevoked: Boolean = false,
)

/**
 * A group that does not exist yet: everything its creator chose, and nothing the group only gets
 * by being written — its id, its members, its admins.
 */
data class NewGroup(
    val name: String,
    val color: String?,
    val types: List<GroupEventType>,
    val invitationCode: String,
    val autoApprove: Boolean,
)

data class GroupMember(
    val id: UserId,
    val name: String,
    val username: String,
    val isAdmin: Boolean,
)

/** Somebody who used the code while the group was not auto-approving. */
data class JoinRequest(
    val userId: UserId,
    val name: String,
    val username: String,
)

data class MyJoinRequest(
    val groupId: GroupId,
    val groupName: String,
    val status: JoinRequestStatus,
)

enum class JoinRequestStatus { PENDING, ACCEPTED, REJECTED }
