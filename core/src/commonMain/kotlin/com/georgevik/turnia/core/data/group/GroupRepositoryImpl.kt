package com.georgevik.turnia.core.data.group

import com.georgevik.turnia.core.data.datasource.firestore.GroupEventFirestore
import com.georgevik.turnia.core.data.datasource.firestore.GroupFirestore
import com.georgevik.turnia.core.data.datasource.firestore.GroupJoinRequestFirestore
import com.georgevik.turnia.core.data.datasource.firestore.RevokedGroupFirestore
import com.georgevik.turnia.core.data.datasource.firestore.UserPathFirestore
import com.georgevik.turnia.core.data.datasource.firestore.UserPrivateFirestore
import com.georgevik.turnia.core.data.datasource.firestore.doc.DocHolder
import com.georgevik.turnia.core.data.datasource.firestore.doc.GroupDocument
import com.georgevik.turnia.core.data.datasource.firestore.doc.GroupMemberDocument
import com.georgevik.turnia.core.data.datasource.firestore.mappers.GroupMapper
import com.georgevik.turnia.core.data.datasource.firestorefunctions.GroupFunction
import com.georgevik.turnia.core.data.datasource.firestorefunctions.GroupMembershipFunction
import com.georgevik.turnia.core.data.logger.Logger
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
import com.georgevik.turnia.core.domain.model.MyJoinRequest
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.domain.repository.AppConfigRepository
import com.georgevik.turnia.core.domain.repository.GroupRepository
import com.georgevik.turnia.core.domain.repository.UserRepository
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.createId
import com.georgevik.turnia.core.system.errorOrNull
import com.georgevik.turnia.core.system.map
import com.georgevik.turnia.core.system.mapError
import com.georgevik.turnia.core.system.onSuccess
import com.georgevik.turnia.core.system.toFailure
import com.georgevik.turnia.core.system.toInstant
import com.georgevik.turnia.core.system.toSuccess
import com.georgevik.turnia.core.system.valueOrNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

@OptIn(ExperimentalCoroutinesApi::class)
class GroupRepositoryImpl(
    private val userRepository: UserRepository,
    private val appConfigRepository: AppConfigRepository,
    private val invitationCodeFactory: InvitationCodeFactory,
    private val groupFirestore: GroupFirestore,
    private val groupEventFirestore: GroupEventFirestore,
    private val groupJoinRequestFirestore: GroupJoinRequestFirestore,
    private val userPrivateFirestore: UserPrivateFirestore,
    private val revokedGroupFirestore: RevokedGroupFirestore,
    private val groupMembershipFunction: GroupMembershipFunction,
    private val groupFunction: GroupFunction,
    private val userPathFirestore: UserPathFirestore,
    private val groupMapper: GroupMapper,
) : GroupRepository {

    /**
     * The groups the user belongs to, plus the ones they were removed from while still holding
     * events. The second kind comes from the user's own snapshots, not from `groups`: a revoked
     * user cannot read that document any more.
     */
    override fun getGroups(): Flow<List<Group>> =
        userRepository.loggedUserFlow.flatMapLatest { user ->
            val userId = user.id
            val colors = typeColors(userId)

            combine(
                groupFirestore.observeMyGroups(userId),
                revokedGroupFirestore.observe(userId),
            ) { mine, revoked ->
                mine.map { groupMapper.map(it, userId, colors) } +
                        revoked.map { groupMapper.map(it, colors) }
            }
        }

    override fun createInvitationCode(): String =
        invitationCodeFactory.create(appConfigRepository.featureFlags.value.invitationCodeLength)

    override suspend fun getGroup(groupId: GroupId): Outcome<Group, GroupError> {
        val userId = userRepository.loggedUser?.id ?: return GroupError.NotFound.toFailure()
        val holder =
            groupFirestore.get(groupId).valueOrNull() ?: return GroupError.NotFound.toFailure()

        return groupMapper.map(holder, userId, typeColors(userId)).toSuccess()
    }

    override suspend fun saveGroup(group: Group): Outcome<Group, GroupError> {
        val user = userRepository.loggedUser ?: return GroupError.NotFound.toFailure()
        val userId = user.id
        val isNew = group.id.value.isBlank()

        val current = if (isNew) null else groupFirestore.get(group.id).valueOrNull()
        val groupId = if (isNew) GroupId(createId()) else group.id
        val memberUids = current?.doc?.memberUids ?: listOf(userId.value)
        val adminUids = current?.doc?.adminUids ?: listOf(userId.value)
        val revokedUids = current?.doc?.revokedUids.orEmpty()
        // The creator is a member from the start, so their name has to be here too: the calendar
        // reads it off the group and nothing else would ever add it.
        val members = current?.doc?.members ?: mapOf(
            userId.value to GroupMemberDocument(
                name = user.displayName.orEmpty(),
                username = user.username,
            )
        )

        // A group without a code is a group nobody can join. The screens mint one up front so the
        // admin can read it before saving; this is what catches a group that arrived without.
        val coded =
            if (group.invitationCode.isBlank()) group.copy(invitationCode = createInvitationCode())
            else group

        val document = groupMapper.map(coded, memberUids, adminUids, revokedUids)
            .copy(members = members)

        val saved = if (isNew) groupFirestore.create(groupId, document)
        else groupFirestore.update(groupId, document)

        saved.errorOrNull()?.let { error ->
            Logger.e(TAG, "Failed to save group: $error")
            return GroupError.NotFound.toFailure()
        }

        return coded.copy(id = groupId, isAdmin = userId.value in adminUids).toSuccess()
    }

    override suspend fun getJoinRequests(groupId: GroupId): Outcome<List<JoinRequest>, GroupError> =
        groupJoinRequestFirestore.getPending(groupId)
            .map { requests -> requests.map(groupMapper::map) }
            .mapError { error ->
                Logger.e(TAG, "Failed to read the join requests: $error")
                GroupError.LoadFailed
            }

    override suspend fun acceptJoinRequest(
        groupId: GroupId,
        userId: UserId,
    ): Outcome<Unit, GroupError> = groupMembershipFunction.acceptJoinRequest(groupId, userId)

    override suspend fun requestToJoinGroup(code: String): Outcome<JoinGroupStatus, JoinGroupError> =
        groupMembershipFunction.requestToJoinGroup(code)

    override suspend fun leaveGroup(groupId: GroupId): Outcome<Unit, GroupError> =
        groupMembershipFunction.leaveGroup(groupId)

    override suspend fun removeMember(
        groupId: GroupId,
        userId: UserId,
    ): Outcome<Unit, GroupError> = groupMembershipFunction.removeMember(groupId, userId)

    override suspend fun deleteGroup(groupId: GroupId): Outcome<Unit, GroupError> =
        groupFunction.deleteGroup(groupId)

    override suspend fun rejectJoinRequest(
        groupId: GroupId,
        userId: UserId,
    ): Outcome<Unit, GroupError> = groupMembershipFunction.rejectJoinRequest(groupId, userId)

    /**
     * One read for the pointer list and one per request it names. A pointer to a request that is no
     * longer there — the group was deleted, or another device already acknowledged it — is dropped
     * instead of reported: nothing else prunes the list.
     */
    override suspend fun getMyJoinRequests(): Outcome<List<MyJoinRequest>, GroupError> {
        val userId = userRepository.loggedUser?.id ?: return GroupError.LoadFailed.toFailure()

        val groupIds = userPrivateFirestore.fetchJoinRequests(userId).valueOrNull()
            ?: run {
                Logger.e(TAG, "Failed to read the join request pointers")
                return GroupError.LoadFailed.toFailure()
            }

        return groupIds.mapNotNull { groupId ->
            val doc = groupJoinRequestFirestore.fetch(GroupId(groupId), userId).valueOrNull()
            if (doc == null) {
                userPrivateFirestore.removeJoinRequest(userId, groupId)
                null
            } else {
                groupMapper.map(GroupId(groupId), doc)
            }
        }.toSuccess()
    }

    override suspend fun acknowledgeJoinRequest(groupId: GroupId): Outcome<Unit, GroupError> {
        val userId = userRepository.loggedUser?.id ?: return GroupError.SaveFailed.toFailure()

        // The pointer only once the request is actually gone: dropped first, it would strand a
        // request nothing can reach, whereas one left behind finds nothing and prunes itself.
        return groupJoinRequestFirestore.delete(groupId, userId)
            .mapError { error ->
                Logger.e(TAG, "Failed to delete the join request: $error")
                GroupError.SaveFailed
            }
            .onSuccess { userPrivateFirestore.removeJoinRequest(userId, groupId.value) }
    }

    override suspend fun saveEventType(
        groupId: GroupId,
        type: GroupEventType,
    ): Outcome<Unit, GroupError> {
        val group = when (val outcome = getGroup(groupId)) {
            is Outcome.Failure -> return outcome
            is Outcome.Success -> outcome.value
        }

        val types = group.types.filterNot { it.id == type.id } + type

        return saveGroup(group.copy(types = types)).map { }
    }

    override suspend fun saveTypeColor(
        groupId: GroupId,
        typeId: EventTypeId,
        color: String
    ): Result<Unit> {
        val userId = userRepository.loggedUser?.id
            ?: return Result.failure(IllegalStateException("No signed-in user"))

        return userPathFirestore.updateTypeColor(userId, groupId, typeId, color).errorOrNull()
            ?.let { Result.failure(IllegalStateException("Failed to save the colour: $it")) }
            ?: Result.success(Unit)
    }

    override suspend fun addEvent(event: GroupEvent) {
        // Checked here to fail before the write; the security rules refuse it anyway, which is what
        // actually keeps someone who left a group from adding to its calendar.
        val userId = userRepository.loggedUser?.id
        if (userId != null && isRevoked(userId, event.groupId)) {
            Logger.w(TAG, "A revoked member cannot add events to the group")
            return
        }

        groupEventFirestore.set(event.groupId, event.id, groupMapper.map(event))
    }

    private suspend fun isRevoked(userId: UserId, groupId: GroupId): Boolean =
        revokedGroupFirestore.observe(userId).first().any { it.id == groupId.value }

    override suspend fun deleteEvent(
        groupId: GroupId,
        eventId: EventId,
        eventDate: LocalDate,
        ownerId: UserId,
        assigneeId: UserId
    ): Outcome<Unit, Unit> {
        // Checked here to fail before the write and with something to show the user; the security
        // rules refuse it anyway, which is what actually keeps a member from deleting another's.
        val userId = userRepository.loggedUser?.id
        if (userId != ownerId || userId != assigneeId) {
            Logger.w(TAG, "Only the creator still holding a shift can delete it")
            return Unit.toFailure()
        }

        return groupEventFirestore.delete(groupId, eventId, eventDate)
            .mapError { error -> Logger.e(TAG, "Failed to delete the group event: $error") }
    }

    /**
     * Follows the group as well as its events: a type renamed or recoloured, or a member renamed,
     * re-renders the calendar without a reload, because the shifts are drawn from both.
     */
    override fun getEventsByGroup(
        groupId: GroupId,
        date: LocalDate,
        monthDelta: Int,
    ): Flow<Outcome<List<GroupEvent>, Unit>> = userRepository.loggedUserFlow.flatMapLatest { user ->
        val userId = user.id
        val colors = typeColors(userId)

        // A revoked user cannot read the group document, so the group they see is their own
        // snapshot of it; falling through to `observe` would only earn a permission error.
        revokedGroupFirestore.observe(userId).flatMapLatest { revoked ->
            val snapshot = revoked.find { it.id == groupId.value }
            if (snapshot != null) {
                val group = groupMapper.map(snapshot, colors)
                eventsOf(group, emptyMap(), userId, date, monthDelta).map { it.toSuccess() }
            } else {
                groupFirestore.observe(groupId).flatMapLatest { holder ->
                    if (holder == null) flowOf(Unit.toFailure())
                    else eventsOf(holder, userId, colors, date, monthDelta).map { it.toSuccess() }
                }
            }
        }
    }

    /**
     * The user's own shifts across every group. One pass over the groups [getGroups] already
     * returns — each carries its types and its members, so nothing else has to be read to render
     * them — and that includes the groups they were removed from: the shifts they still hold are
     * theirs to cover, so they belong on their calendar.
     */
    override fun getEventsByUser(
        userId: UserId,
        date: LocalDate,
        monthDelta: Int,
    ): Flow<List<GroupEvent>> = flow {
        val viewer = userRepository.loggedUser?.id
        if (viewer == null) {
            emit(emptyList())
            return@flow
        }

        // Following the groups too: joining one, or an admin renaming a type, reaches the calendar
        // without a reload — the shifts are drawn from the group as much as from the events.
        emitAll(
            getGroups().flatMapLatest { groups ->
                if (groups.isEmpty()) return@flatMapLatest flowOf(emptyList())

                // Combined, so a group answering from cache paints while another is on the wire.
                val perGroup = groups.map { group ->
                    eventsOf(group, group.memberNames(), viewer, date, monthDelta)
                }
                combine(perGroup) { events ->
                    events.toList().flatten()
                        .filter { it.assigneeId == userId || it.ownerId == userId }
                }
            }
        )
    }

    /** The copy of the members' names the group carries, so rendering one costs no read. */
    private fun Group.memberNames(): Map<String, String> =
        members.associate { member -> member.id.value to member.name }

    /** Cached events first, then the merged ones when a month turned out to be behind. */
    private fun eventsOf(
        holder: DocHolder<GroupDocument>,
        viewer: UserId,
        colors: Map<String, String>,
        date: LocalDate,
        monthDelta: Int,
    ): Flow<List<GroupEvent>> = eventsOf(
        group = groupMapper.map(holder, viewer, colors),
        memberNames = holder.doc.members.mapValues { (_, member) -> member.name },
        viewer = viewer,
        date = date,
        monthDelta = monthDelta,
    )

    private fun eventsOf(
        group: Group,
        memberNames: Map<String, String>,
        viewer: UserId,
        date: LocalDate,
        monthDelta: Int,
    ): Flow<List<GroupEvent>> = groupEventFirestore.get(
        group.id,
        from = date.minus(monthDelta, DateTimeUnit.MONTH).toInstant(),
        until = date.plus(monthDelta, DateTimeUnit.MONTH).toInstant(),
        // Not an optimisation: the rules only let a revoked user read the events assigned to them,
        // and they prove it from the query's filters, so without this the read is refused.
        assigneeId = viewer.takeIf { group.isRevoked },
    ).map { outcome ->
        outcome.valueOrNull().orEmpty().mapNotNull { groupMapper.map(it, group, memberNames) }
    }

    /** From the cache: the colours are the user's own picks, and every group screen asks. */
    private suspend fun typeColors(userId: UserId): Map<String, String> =
        userPathFirestore.getCachedUserDocument(userId).valueOrNull()
            ?.groupEventTypeColors.orEmpty()

    companion object {
        private const val TAG = "GroupRepository"
    }
}
