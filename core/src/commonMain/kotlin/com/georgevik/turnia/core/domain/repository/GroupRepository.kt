package com.georgevik.turnia.core.domain.repository

import com.georgevik.turnia.core.domain.model.EventId
import com.georgevik.turnia.core.domain.model.EventTypeId
import com.georgevik.turnia.core.domain.model.Group
import com.georgevik.turnia.core.domain.model.GroupError
import com.georgevik.turnia.core.domain.model.GroupEvent
import com.georgevik.turnia.core.domain.model.GroupEventType
import com.georgevik.turnia.core.domain.model.GroupId
import com.georgevik.turnia.core.domain.model.JoinGroupError
import com.georgevik.turnia.core.domain.model.JoinGroupStatus
import com.georgevik.turnia.core.domain.model.JoinRequest
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.system.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface GroupRepository {

    /** The user's groups, and again whenever one of them changes. */
    fun getGroups(): Flow<List<Group>>

    fun createInvitationCode(): String

    suspend fun addEvent(event: GroupEvent)

    /** A shift is only deletable by whoever created it *and* still holds it. */
    suspend fun deleteEvent(
        groupId: GroupId,
        eventId: EventId,
        eventDate: LocalDate,
        ownerId: UserId,
        assigneeId: UserId
    ): Outcome<Unit, Unit>

    /** Cached events first, then the server's if it had anything newer. */
    fun getEventsByGroup(
        groupId: GroupId,
        date: LocalDate,
        monthDelta: Int = 1,
    ): Flow<Outcome<List<GroupEvent>, Unit>>

    /** Cached events first, then the server's if it had anything newer. */
    fun getEventsByUser(
        userId: UserId,
        date: LocalDate,
        monthDelta: Int = 1,
    ): Flow<List<GroupEvent>>

    suspend fun getGroup(groupId: GroupId): Outcome<Group, GroupError>

    /** Creates the group when [group] has a blank id, updates it otherwise. */
    suspend fun saveGroup(group: Group): Outcome<Group, GroupError>

    /** Who is waiting to be let in. Only an admin can read them. */
    suspend fun getJoinRequests(groupId: GroupId): Outcome<List<JoinRequest>, GroupError>

    suspend fun acceptJoinRequest(groupId: GroupId, userId: UserId): Outcome<Unit, GroupError>

    /** Asks to join with an invitation code; auto-approving invitations join on the spot. */
    suspend fun requestToJoinGroup(code: String): Outcome<JoinGroupStatus, JoinGroupError>

    suspend fun rejectJoinRequest(groupId: GroupId, userId: UserId): Outcome<Unit, GroupError>

    /**
     * Leaves the group. Events the user still holds stay behind and they keep read access to those
     * alone; the only admin of a group with members left in it is refused.
     */
    suspend fun leaveGroup(groupId: GroupId): Outcome<Unit, GroupError>

    /** Removes a member, the same way [leaveGroup] does but decided by an admin. */
    suspend fun removeMember(groupId: GroupId, userId: UserId): Outcome<Unit, GroupError>

    suspend fun deleteGroup(groupId: GroupId): Outcome<Unit, GroupError>

    /** Adds the event type to the group, or replaces the one with the same id. Admins only. */
    suspend fun saveEventType(groupId: GroupId, type: GroupEventType): Outcome<Unit, GroupError>

    suspend fun saveTypeColor(groupId: GroupId, typeId: EventTypeId, color: String) : Result<Unit>
}
