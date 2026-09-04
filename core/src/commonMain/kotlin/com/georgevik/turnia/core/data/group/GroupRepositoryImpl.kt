package com.georgevik.turnia.core.data.group

import com.georgevik.turnia.core.data.datasource.firestore.GroupEventFirestore
import com.georgevik.turnia.core.data.datasource.firestore.GroupFirestore
import com.georgevik.turnia.core.data.datasource.firestore.GroupJoinRequestFirestore
import com.georgevik.turnia.core.data.datasource.firestore.UserPathFirestore
import com.georgevik.turnia.core.data.datasource.firestore.doc.DocHolder
import com.georgevik.turnia.core.data.datasource.firestore.doc.GroupDocument
import com.georgevik.turnia.core.data.datasource.firestore.doc.GroupMemberDocument
import com.georgevik.turnia.core.data.datasource.firestore.mappers.GroupMapper
import com.georgevik.turnia.core.data.datasource.firestorefunctions.GroupMembershipFunction
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.EventId
import com.georgevik.turnia.core.domain.model.EventTypeId
import com.georgevik.turnia.core.domain.model.Group
import com.georgevik.turnia.core.domain.model.GroupError
import com.georgevik.turnia.core.domain.model.GroupEvent
import com.georgevik.turnia.core.domain.model.GroupEventType
import com.georgevik.turnia.core.domain.model.GroupId
import com.georgevik.turnia.core.domain.model.JoinRequest
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.domain.repository.GroupRepository
import com.georgevik.turnia.core.domain.repository.UserRepository
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.createId
import com.georgevik.turnia.core.system.createInvitationCode
import com.georgevik.turnia.core.system.errorOrNull
import com.georgevik.turnia.core.system.map
import com.georgevik.turnia.core.system.mapError
import com.georgevik.turnia.core.system.toFailure
import com.georgevik.turnia.core.system.toInstant
import com.georgevik.turnia.core.system.toSuccess
import com.georgevik.turnia.core.system.valueOrNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
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
    private val groupFirestore: GroupFirestore,
    private val groupEventFirestore: GroupEventFirestore,
    private val groupJoinRequestFirestore: GroupJoinRequestFirestore,
    private val groupMembershipFunction: GroupMembershipFunction,
    private val userPathFirestore: UserPathFirestore,
    private val groupMapper: GroupMapper,
) : GroupRepository {

    override fun getGroups(): Flow<List<Group>> = flow {
        val userId = userRepository.loggedUser?.id
        if (userId == null) {
            emit(emptyList())
            return@flow
        }

        val colors = typeColors(userId)
        emitAll(
            groupFirestore.observeMyGroups(userId).map { holders ->
                holders.map { groupMapper.map(it, userId, colors) }
            }
        )
    }

    override suspend fun getGroup(groupId: GroupId): Outcome<Group, GroupError> {
        val userId = userRepository.loggedUser?.id ?: return GroupError.NotFound.toFailure()
        val holder = groupFirestore.get(groupId).valueOrNull() ?: return GroupError.NotFound.toFailure()

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
        // The creator is a member from the start, so their name has to be here too: the calendar
        // reads it off the group and nothing else would ever add it.
        val members = current?.doc?.members ?: mapOf(
            userId.value to GroupMemberDocument(
                name = user.displayName.orEmpty(),
                username = user.username,
            )
        )

        val invitationCode = group.invitationCode ?: createInvitationCode()
        val document = groupMapper.map(group.copy(invitationCode = invitationCode), memberUids, adminUids)
            .copy(members = members)

        val saved = if (isNew) groupFirestore.create(groupId, document)
        else groupFirestore.update(groupId, document)

        saved.errorOrNull()?.let { error ->
            Logger.e(TAG, "Failed to save group: $error")
            return GroupError.NotFound.toFailure()
        }

        return group.copy(id = groupId, invitationCode = invitationCode, isAdmin = userId.value in adminUids)
            .toSuccess()
    }

    override suspend fun getJoinRequests(groupId: GroupId): Outcome<List<JoinRequest>, GroupError> =
        groupJoinRequestFirestore.get(groupId)
            .map { requests -> requests.map(groupMapper::map) }
            .mapError { error ->
                Logger.e(TAG, "Failed to read the join requests: $error")
                GroupError.LoadFailed
            }

    override suspend fun acceptJoinRequest(
        groupId: GroupId,
        userId: UserId,
    ): Outcome<Unit, GroupError> = groupMembershipFunction.acceptJoinRequest(groupId, userId)

    override suspend fun rejectJoinRequest(
        groupId: GroupId,
        userId: UserId,
    ): Outcome<Unit, GroupError> = groupJoinRequestFirestore.delete(groupId, userId)
        .mapError { error ->
            Logger.e(TAG, "Failed to reject the join request: $error")
            GroupError.SaveFailed
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
        groupEventFirestore.set(event.groupId, event.id, groupMapper.map(event))
    }

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
    ): Flow<Outcome<List<GroupEvent>, Unit>> = flow {
        val userId = userRepository.loggedUser?.id
        if (userId == null) {
            emit(Unit.toFailure())
            return@flow
        }

        val colors = typeColors(userId)
        emitAll(
            groupFirestore.observe(groupId).flatMapLatest { holder ->
                if (holder == null) flowOf(Unit.toFailure())
                else eventsOf(holder, userId, colors, date, monthDelta).map { it.toSuccess() }
            }
        )
    }

    /**
     * The user's own shifts across every group they belong to. One pass over the groups the "my
     * groups" query already returns — each carries its types and its members, so nothing else has
     * to be read to render them.
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

        val colors = typeColors(viewer)

        // Following the groups too: joining one, or an admin renaming a type, reaches the calendar
        // without a reload — the shifts are drawn from the group as much as from the events.
        emitAll(
            groupFirestore.observeMyGroups(viewer).flatMapLatest { groups ->
                if (groups.isEmpty()) return@flatMapLatest flowOf(emptyList())

                // Combined, so a group answering from cache paints while another is on the wire.
                val perGroup = groups.map { holder ->
                    eventsOf(holder, viewer, colors, date, monthDelta)
                }
                combine(perGroup) { events ->
                    events.toList().flatten()
                        .filter { it.assigneeId == userId || it.ownerId == userId }
                }
            }
        )
    }

    /** Cached events first, then the merged ones when a month turned out to be behind. */
    private fun eventsOf(
        holder: DocHolder<GroupDocument>,
        viewer: UserId,
        colors: Map<String, String>,
        date: LocalDate,
        monthDelta: Int,
    ): Flow<List<GroupEvent>> {
        val group = groupMapper.map(holder, viewer, colors)
        val memberNames = holder.doc.members.mapValues { (_, member) -> member.name }

        return groupEventFirestore.get(
            group.id,
            from = date.minus(monthDelta, DateTimeUnit.MONTH).toInstant(),
            until = date.plus(monthDelta, DateTimeUnit.MONTH).toInstant(),
        ).map { outcome ->
            outcome.valueOrNull().orEmpty().mapNotNull { groupMapper.map(it, group, memberNames) }
        }
    }

    private suspend fun typeColors(userId: UserId): Map<String, String> =
        userPathFirestore.getUserDocument(userId).valueOrNull()?.groupEventTypeColors.orEmpty()

    companion object {
        private const val TAG = "GroupRepository"
    }
}
