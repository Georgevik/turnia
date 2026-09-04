package com.georgevik.turnia.core.domain.model

data class Group(
    val id: GroupId,
    val name: String,
    val types: List<GroupEventType>,
    val members: List<GroupMember>,
    val memberCount: Int,
    val invitationCode: String?,
    /** Whoever knows the code walks in; otherwise an admin has to accept the request. */
    val autoApprove: Boolean,
    val membersCanSeeCode: Boolean,
    val isAdmin: Boolean,
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
