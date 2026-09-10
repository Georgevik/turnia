package com.geoviksoft.turnia.core.domain.repository

import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.core.domain.model.EventTypeId
import com.geoviksoft.turnia.core.domain.model.Group
import com.geoviksoft.turnia.core.domain.model.GroupError
import com.geoviksoft.turnia.core.domain.model.GroupEvent
import com.geoviksoft.turnia.core.domain.model.GroupEventType
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.JoinGroupError
import com.geoviksoft.turnia.core.domain.model.JoinGroupStatus
import com.geoviksoft.turnia.core.domain.model.JoinRequest
import com.geoviksoft.turnia.core.domain.model.MyJoinRequest
import com.geoviksoft.turnia.core.domain.model.NewGroup
import com.geoviksoft.turnia.core.domain.model.SwapError
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.system.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
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

    /**
     * Offers a shift for swap, or withdraws the offer.
     *
     * Whoever covers a shift may offer it, including someone who took it from another member —
     * passing it on again is what makes a chain of changes, not a state to forbid.
     */
    suspend fun setOnSwap(
        groupId: GroupId,
        eventId: EventId,
        eventDate: LocalDate,
        assigneeId: UserId,
        swappable: Boolean,
        onSwap: Boolean,
    ): Outcome<Unit, SwapError>

    /**
     * Takes a shift another member offered.
     */
    suspend fun takeEvent(groupId: GroupId, eventId: EventId): Outcome<Unit, SwapError>

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

    /**
     * Every swap-related shift across the user's groups, from [date] forward.
     */
    fun getSwapEvents(date: LocalDate, monthsAhead: Int = 3): Flow<List<GroupEvent>>

    suspend fun getGroup(groupId: GroupId): Outcome<Group, GroupError>

    /** Creates the group, with the logged user as its first member and only admin. */
    suspend fun createGroup(group: NewGroup): Outcome<Group, GroupError>

    suspend fun updateGroup(group: Group): Outcome<Group, GroupError>

    /** Who is waiting to be let in. Only an admin can read them. */
    suspend fun getJoinRequests(groupId: GroupId): Outcome<List<JoinRequest>, GroupError>

    suspend fun acceptJoinRequest(groupId: GroupId, userId: UserId): Outcome<Unit, GroupError>

    /** Asks to join with an invitation code; auto-approving invitations join on the spot. */
    suspend fun requestToJoinGroup(code: String): Outcome<JoinGroupStatus, JoinGroupError>

    suspend fun rejectJoinRequest(groupId: GroupId, userId: UserId): Outcome<Unit, GroupError>

    /**
     * The logged user's own requests, pending and answered alike.
     *
     * Reads their pointer list and then each request it names: a requester may read their own
     * request by id, and there is no query that can do the same.
     */
    suspend fun getMyJoinRequests(): Flow<List<MyJoinRequest>>

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

    /**
     * Event types built for a group that does not exist yet.
     *
     * A group's types live on the group's own document, so until the group is created there is
     * nowhere to write one: they are held here and the group is created with the whole lot in the
     * same write, never existing without them.
     */
    val pendingEventTypes: StateFlow<List<GroupEventType>>

    /** Holds a type for the group being created, or replaces the one it is an edit of. */
    fun setPendingEventType(type: GroupEventType)

    /** Hands over the types held for the group being created and forgets them. */
    fun consumePendingEventTypes(): List<GroupEventType>
}
