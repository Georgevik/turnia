package com.georgevik.turnia.core.data.datasource.firestore.mappers

import com.georgevik.turnia.core.data.datasource.firestore.doc.DocHolder
import com.georgevik.turnia.core.data.datasource.firestore.doc.EventHistoryDocument
import com.georgevik.turnia.core.data.datasource.firestore.doc.GroupDocument
import com.georgevik.turnia.core.data.datasource.firestore.doc.GroupEventDocument
import com.georgevik.turnia.core.data.datasource.firestore.doc.GroupEventTypeDocument
import com.georgevik.turnia.core.data.datasource.firestore.doc.InvitationDocument
import com.georgevik.turnia.core.data.datasource.firestore.doc.JoinRequestDocument
import com.georgevik.turnia.core.data.datasource.firestore.doc.JoinRequestStatusDocument
import com.georgevik.turnia.core.data.datasource.firestore.doc.RevokedGroupDocument
import com.georgevik.turnia.core.data.datasource.firestore.doc.UserDocument
import com.georgevik.turnia.core.domain.model.EventHistoryEntry
import com.georgevik.turnia.core.domain.model.EventId
import com.georgevik.turnia.core.domain.model.EventTypeId
import com.georgevik.turnia.core.domain.model.Group
import com.georgevik.turnia.core.domain.model.GroupEvent
import com.georgevik.turnia.core.domain.model.GroupEventType
import com.georgevik.turnia.core.domain.model.GroupId
import com.georgevik.turnia.core.domain.model.GroupMember
import com.georgevik.turnia.core.domain.model.JoinRequest
import com.georgevik.turnia.core.domain.model.JoinRequestStatus
import com.georgevik.turnia.core.domain.model.MyJoinRequest
import com.georgevik.turnia.core.domain.model.UserId
import kotlinx.datetime.LocalDate
import kotlinx.datetime.yearMonth

/**
 * Group documents to the domain and back.
 */
class GroupMapper {

    fun map(holder: DocHolder<GroupDocument>, viewer: UserId, colors: Map<String, String>): Group {
        val groupId = GroupId(holder.id)
        val doc = holder.doc

        val isAdmin = viewer.value in doc.adminUids
        val invitation = doc.invitation

        return Group(
            id = groupId,
            name = doc.name,
            types = doc.groupEventTypes.map { map(it, groupId, doc.name, colors) },
            members = doc.members.map { (uid, member) ->
                GroupMember(
                    id = UserId(uid),
                    name = member.name,
                    username = member.username,
                    isAdmin = uid in doc.adminUids,
                )
            }.sortedByDescending { it.isAdmin },
            // The uids are the source of truth for membership; the names are a copy that a member
            // who joined before the group started keeping them may still be missing from.
            memberCount = doc.memberUids.size,
            invitationCode = invitation.code
                .takeIf { isAdmin || invitation.membersCanSeeCode }.orEmpty(),
            autoApprove = invitation.autoApprove,
            membersCanSeeCode = invitation.membersCanSeeCode,
            isAdmin = isAdmin,
            color = doc.color,
        )
    }

    /**
     * The snapshot left behind when the user was removed, as a [Group] the calendar can render like
     * any other. There is no roster and no invitation: what survives is the name and the types the
     * user's own leftover events use.
     */
    fun map(holder: DocHolder<RevokedGroupDocument>, colors: Map<String, String>): Group {
        val groupId = GroupId(holder.id)
        val doc = holder.doc

        return Group(
            id = groupId,
            name = doc.name,
            types = doc.groupEventTypes.map { map(it, groupId, doc.name, colors) },
            members = emptyList(),
            memberCount = 0,
            // Blank, the same as a code hidden from a member: a revoked user has no way in and
            // nothing to hand out. Their snapshot does not carry the invitation either.
            invitationCode = "",
            autoApprove = false,
            membersCanSeeCode = false,
            isAdmin = false,
            color = doc.color,
            isRevoked = true,
        )
    }

    fun map(
        group: Group,
        memberUids: List<String>,
        adminUids: List<String>,
        revokedUids: List<String>,
    ) = GroupDocument(
        name = group.name,
        color = group.color,
        memberUids = memberUids,
        revokedUids = revokedUids,
        adminUids = adminUids,
        groupEventTypes = group.types.map(::map),
        invitation = InvitationDocument(
            code = group.invitationCode,
            autoApprove = group.autoApprove,
            membersCanSeeCode = group.membersCanSeeCode,
        ),
    )

    fun map(holder: DocHolder<JoinRequestDocument>) = JoinRequest(
        userId = UserId(holder.id),
        name = holder.doc.name,
        username = holder.doc.username,
    )

    fun map(groupId: GroupId, doc: JoinRequestDocument) = MyJoinRequest(
        groupId = groupId,
        groupName = doc.groupName,
        status = when (doc.status) {
            JoinRequestStatusDocument.PENDING -> JoinRequestStatus.PENDING
            JoinRequestStatusDocument.ACCEPTED -> JoinRequestStatus.ACCEPTED
            JoinRequestStatusDocument.REJECTED -> JoinRequestStatus.REJECTED
        },
    )

    fun map(
        holder: DocHolder<GroupEventDocument>,
        group: Group,
        members: Map<String, String>,
    ): GroupEvent? {
        val doc = holder.doc
        // An event whose type the admin removed has nothing left to render.
        val type = group.types.find { it.id.value == doc.groupEventTypeId } ?: return null

        return GroupEvent(
            id = EventId(holder.id),
            groupId = group.id,
            groupName = group.name,
            ownerId = UserId(doc.ownerId),
            assigneeId = UserId(doc.assigneeId),
            assigneeName = members[doc.assigneeId].orEmpty(),
            type = type,
            date = LocalDate.parse(doc.date),
            onSwap = doc.onSwap,
            colorHex = type.color,
            history = chainOf(doc, members),
        )
    }

    /**
     * Who has held this shift, in order: the creator first, then whoever each transfer handed it
     * to. The names come from the group's own member list, so the chain costs nothing to render.
     */
    private fun chainOf(
        doc: GroupEventDocument,
        members: Map<String, String>
    ): List<EventHistoryEntry> {
        val holders = listOf(doc.ownerId) + doc.history
            .filter { it.type == EventHistoryDocument.TYPE_TRANSFERRED }
            .mapNotNull { it.toUid }

        return holders.map { uid ->
            EventHistoryEntry(userId = UserId(uid), userName = members[uid].orEmpty())
        }
    }

    fun map(event: GroupEvent) = GroupEventDocument(
        ownerId = event.ownerId.value,
        assigneeId = event.assigneeId.value,
        groupEventTypeId = event.type.id.value,
        date = event.date.toString(),
        yearMonth = event.date.yearMonth.toString(),
        onSwap = event.onSwap,
    )

    private fun map(
        doc: GroupEventTypeDocument,
        groupId: GroupId,
        groupName: String,
        colors: Map<String, String>,
    ) = GroupEventType(
        id = EventTypeId(doc.id),
        groupId = groupId,
        groupName = groupName,
        name = doc.name,
        acronym = doc.acronym,
        description = doc.description,
        startTime = doc.startTime,
        endTime = doc.endTime,
        swappable = doc.swappable,
        officialColor = doc.color.orEmpty(),
        userColor = colors[UserDocument.typeColorKey(groupId.value, doc.id)],
    )

    private fun map(type: GroupEventType) = GroupEventTypeDocument(
        id = type.id.value,
        name = type.name,
        acronym = type.acronym,
        description = type.description,
        startTime = type.startTime,
        endTime = type.endTime,
        swappable = type.swappable,
        color = type.officialColor.ifBlank { null },
    )
}
