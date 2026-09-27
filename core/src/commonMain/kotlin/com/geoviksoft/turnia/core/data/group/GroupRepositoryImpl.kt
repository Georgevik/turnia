package com.geoviksoft.turnia.core.data.group

import com.geoviksoft.turnia.core.data.datasource.firestore.GroupEventFirestore
import com.geoviksoft.turnia.core.data.datasource.firestore.GroupFirestore
import com.geoviksoft.turnia.core.data.datasource.firestore.GroupJoinRequestFirestore
import com.geoviksoft.turnia.core.data.datasource.firestore.RevokedGroupFirestore
import com.geoviksoft.turnia.core.data.datasource.firestore.UserPrivateFirestore
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.DocHolder
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.GroupDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.GroupMemberDocument
import com.geoviksoft.turnia.core.data.datasource.firestorefunctions.GroupEventFunction
import com.geoviksoft.turnia.core.data.datasource.firestorefunctions.GroupFunction
import com.geoviksoft.turnia.core.data.datasource.firestorefunctions.GroupMembershipFunction
import com.geoviksoft.turnia.core.data.group.mappers.GroupMapper
import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.domain.analytics.Analytics
import com.geoviksoft.turnia.core.domain.analytics.AnalyticsEvent
import com.geoviksoft.turnia.core.domain.analytics.AnalyticsUserProperty
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
import com.geoviksoft.turnia.core.domain.repository.AppConfigRepository
import com.geoviksoft.turnia.core.domain.repository.GroupRepository
import com.geoviksoft.turnia.core.domain.repository.UserRepository
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.createId
import com.geoviksoft.turnia.core.system.errorOrNull
import com.geoviksoft.turnia.core.system.isSuccess
import com.geoviksoft.turnia.core.system.map
import com.geoviksoft.turnia.core.system.mapError
import com.geoviksoft.turnia.core.system.onFailure
import com.geoviksoft.turnia.core.system.onSuccess
import com.geoviksoft.turnia.core.system.toFailure
import com.geoviksoft.turnia.core.system.toInstant
import com.geoviksoft.turnia.core.system.toSuccess
import com.geoviksoft.turnia.core.system.valueOrNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.withIndex
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

@OptIn(ExperimentalCoroutinesApi::class)
class GroupRepositoryImpl(
    private val userRepository: UserRepository,
    private val appConfigRepository: AppConfigRepository,
    private val invitationCodeFactory: InvitationCodeFactory,
    private val groupFactory: GroupFactory,
    private val groupFirestore: GroupFirestore,
    private val groupEventFirestore: GroupEventFirestore,
    private val groupJoinRequestFirestore: GroupJoinRequestFirestore,
    private val userPrivateFirestore: UserPrivateFirestore,
    private val revokedGroupFirestore: RevokedGroupFirestore,
    private val groupMembershipFunction: GroupMembershipFunction,
    private val groupFunction: GroupFunction,
    private val groupEventFunction: GroupEventFunction,
    private val groupMapper: GroupMapper,
    private val analytics: Analytics,
) : GroupRepository {

    private val _pendingEventTypes = MutableStateFlow<List<GroupEventType>>(emptyList())
    override val pendingEventTypes: StateFlow<List<GroupEventType>> =
        _pendingEventTypes.asStateFlow()

    private val reportedMembership = MutableStateFlow<Pair<Int, Boolean>?>(null)

    /**
     * The groups the user belongs to, plus the ones they were removed from while still holding
     * events. The second kind comes from the user's own snapshots, not from `groups`: a revoked
     * user cannot read that document any more.
     */
    override fun getGroups(): Flow<List<Group>> =
        userRepository.loggedUserFlow.flatMapLatest { user ->
            val userId = user.id

            combine(
                groupFirestore.observeMyGroups(userId),
                revokedGroupFirestore.observe(userId),
                userPrivateFirestore.observePreferences(userId)
            ) { mine, revoked, preferences ->
                val colors = preferences.groupEventTypeColors
                mine.map { groupMapper.map(it, userId, colors) } +
                        revoked.map { groupMapper.map(it, colors) }
            }.onEach(::reportMembership)
        }

    /**
     * Rides on whichever screen is already listening, so the user properties cost no listener of
     * their own. Several collectors see the same groups; only a change is reported.
     */
    private fun reportMembership(groups: List<Group>) {
        val member = groups.filterNot { it.isRevoked }
        val membership = member.size to member.any { it.isAdmin }
        if (reportedMembership.getAndUpdate { membership } == membership) return

        analytics.setUserProperty(AnalyticsUserProperty.GroupCount(membership.first))
        analytics.setUserProperty(AnalyticsUserProperty.IsAdmin(membership.second))
    }

    override fun createInvitationCode(): String =
        invitationCodeFactory.create(appConfigRepository.featureFlags.value.invitationCodeLength)

    override suspend fun getGroup(groupId: GroupId): Outcome<Group, GroupError> {
        val userId = userRepository.loggedUser?.id ?: return GroupError.NotFound.toFailure()
        val holder =
            groupFirestore.get(groupId).valueOrNull() ?: return GroupError.NotFound.toFailure()

        return groupMapper.map(holder, userId, typeColors(userId)).toSuccess()
    }

    override suspend fun createGroup(group: NewGroup): Outcome<Group, GroupError> {
        val user = userRepository.loggedUser ?: return GroupError.NotFound.toFailure()
        val userId = user.id
        val created = groupFactory.create(
            group = group,
            id = GroupId(createId()),
            // A group without a code is a group nobody can join. The screen mints one up front so
            // the admin can read it before saving; this catches a group that arrived without.
            invitationCode = group.invitationCode.ifBlank { createInvitationCode() },
        )

        val document = groupMapper.map(
            group = created,
            memberUids = listOf(userId.value),
            adminUids = listOf(userId.value),
            revokedUids = emptyList(),
        ).copy(
            // The creator is a member from the start, so their name has to be here too: the
            // calendar reads it off the group and nothing else would ever add it.
            members = mapOf(
                userId.value to GroupMemberDocument(
                    name = user.displayName.orEmpty(),
                    username = user.username,
                )
            )
        )

        groupFirestore.create(created.id, userId, document).errorOrNull()?.let { error ->
            Logger.e(TAG, "Failed to create group: $error")
            return GroupError.NotFound.toFailure()
        }

        // A group's first types are written with it and never pass through saveEventType.
        created.types.forEach { analytics.log(AnalyticsEvent.GroupEventTypeCreated) }
        analytics.log(AnalyticsEvent.GroupCreated(created.autoApprove, created.types.size))

        return created.toSuccess()
    }

    override suspend fun updateGroup(group: Group): Outcome<Group, GroupError> {
        val user = userRepository.loggedUser ?: return GroupError.NotFound.toFailure()
        val userId = user.id

        val current = groupFirestore.get(group.id).valueOrNull()
        val memberUids = current?.doc?.memberUids ?: listOf(userId.value)
        val adminUids = current?.doc?.adminUids ?: listOf(userId.value)
        val revokedUids = current?.doc?.revokedUids.orEmpty()
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

        groupFirestore.update(group.id, document).errorOrNull()?.let { error ->
            Logger.e(TAG, "Failed to save group: $error")
            return GroupError.NotFound.toFailure()
        }

        return coded.copy(isAdmin = userId.value in adminUids).toSuccess()
    }

    override fun observeGroup(groupId: GroupId): Flow<Outcome<Group, GroupError>> =
        userRepository.loggedUserFlow.flatMapLatest { user ->
            combine(
                groupFirestore.observe(groupId)
                    .withIndex()
                    // Not cached yet says nothing about whether the group exists: wait for the server.
                    .filterNot { (index, holder) -> index == 0 && holder == null }
                    .map { it.value },
                userPrivateFirestore.observePreferences(user.id).map { it.groupEventTypeColors },
            ) { holder, colors ->
                holder?.let { groupMapper.map(it, user.id, colors).toSuccess() }
                    ?: GroupError.NotFound.toFailure()
            }
        }

    override fun observeJoinRequests(groupId: GroupId): Flow<List<JoinRequest>> =
        groupJoinRequestFirestore.observePending(groupId)
            .map { requests -> requests.map(groupMapper::map) }

    override suspend fun acceptJoinRequest(
        groupId: GroupId,
        userId: UserId,
    ): Outcome<Unit, GroupError> = groupMembershipFunction.acceptJoinRequest(groupId, userId)
        .also { if (it.isSuccess) analytics.log(AnalyticsEvent.JoinAccepted) }

    override suspend fun requestToJoinGroup(code: String): Outcome<JoinGroupStatus, JoinGroupError> =
        groupMembershipFunction.requestToJoinGroup(code)
            .also { if (it.isSuccess) analytics.log(AnalyticsEvent.JoinRequested) }

    override suspend fun leaveGroup(groupId: GroupId): Outcome<Unit, GroupError> =
        groupMembershipFunction.leaveGroup(groupId)
            .onSuccess { analytics.log(AnalyticsEvent.GroupLeft) }

    override suspend fun removeMember(
        groupId: GroupId,
        userId: UserId,
    ): Outcome<Unit, GroupError> = groupMembershipFunction.removeMember(groupId, userId)
        .onSuccess { analytics.log(AnalyticsEvent.MemberRemoved) }

    override suspend fun deleteGroup(groupId: GroupId): Outcome<Unit, GroupError> =
        groupFunction.deleteGroup(groupId)
            .onSuccess { analytics.log(AnalyticsEvent.GroupDeleted) }

    override suspend fun rejectJoinRequest(
        groupId: GroupId,
        userId: UserId,
    ): Outcome<Unit, GroupError> = groupMembershipFunction.rejectJoinRequest(groupId, userId)
        .onSuccess { analytics.log(AnalyticsEvent.JoinRequestRejected) }

    /**
     * One read for the pointer list and one per request it names. A pointer to a request that is no
     * longer there — the group was deleted, or another device already acknowledged it — is dropped
     * instead of reported: nothing else prunes the list.
     */
    override suspend fun getMyJoinRequests(): Flow<List<MyJoinRequest>> =
        userRepository.loggedUserFlow.flatMapLatest { user ->
            val userId = user.id

            userPrivateFirestore.fetchJoinRequests(userId).map { groupIds ->
                groupIds.mapNotNull { groupId ->
                    val doc =
                        groupJoinRequestFirestore.observe(GroupId(groupId), userId).valueOrNull()

                    if (doc == null) {
                        userPrivateFirestore.removeJoinRequest(userId, groupId)
                        null
                    } else {
                        groupMapper.map(GroupId(groupId), doc)
                    }
                }
            }
        }

    override fun setPendingEventType(type: GroupEventType) = _pendingEventTypes.update { held ->
        if (held.none { it.id == type.id }) held + type
        else held.map { if (it.id == type.id) type else it }
    }

    override fun consumePendingEventTypes(): List<GroupEventType> =
        _pendingEventTypes.getAndUpdate { emptyList() }

    override suspend fun saveEventType(
        groupId: GroupId,
        type: GroupEventType,
    ): Outcome<Unit, GroupError> {
        val group = when (val outcome = getGroup(groupId)) {
            is Outcome.Failure -> return outcome
            is Outcome.Success -> outcome.value
        }

        val existing = group.types.find { it.id == type.id }
        val saved = if (existing == null) type else type.copy(defaultColor = existing.defaultColor)

        val types = group.types.filterNot { it.id == type.id } + saved

        return updateGroup(group.copy(types = types))
            .onSuccess { if (existing == null) analytics.log(AnalyticsEvent.GroupEventTypeCreated) }
            .map { }
    }

    override suspend fun saveTypeColor(
        groupId: GroupId,
        typeId: EventTypeId,
        color: String
    ): Result<Unit> {
        val userId = userRepository.loggedUser?.id
            ?: return Result.failure(IllegalStateException("No signed-in user"))

        userPrivateFirestore.updateTypeColor(userId, groupId, typeId, color).errorOrNull()
            ?.let { return Result.failure(IllegalStateException("Failed to save the colour: $it")) }

        return Result.success(Unit)
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
            .onSuccess { analytics.log(AnalyticsEvent.GroupEventCreated) }
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
            .onSuccess { analytics.log(AnalyticsEvent.GroupEventDeleted) }
            .mapError { error -> Logger.e(TAG, "Failed to delete the group event: $error") }
    }

    override suspend fun setOnSwap(
        groupId: GroupId,
        eventId: EventId,
        eventDate: LocalDate,
        assigneeId: UserId,
        swappable: Boolean,
        onSwap: Boolean,
    ): Outcome<Unit, SwapError> {
        val userId = userRepository.loggedUser?.id
        if (userId != assigneeId) {
            Logger.w(TAG, "Only whoever covers a shift can offer it")
            return SwapError.NotAssignee.toFailure()
        }

        if (!swappable) {
            Logger.w(TAG, "This event type does not allow swapping")
            return SwapError.NotSwappable.toFailure()
        }

        return groupEventFirestore.updateOnSwap(groupId, eventId, eventDate, onSwap)
            .onSuccess {
                analytics.log(if (onSwap) AnalyticsEvent.SwapOffered else AnalyticsEvent.SwapWithdrawn)
            }
            .mapError { error ->
                Logger.e(TAG, "Failed to set the group event onSwap: $error")
                SwapError.SaveFailed
            }
    }

    override suspend fun takeEvent(groupId: GroupId, eventId: EventId): Outcome<Unit, SwapError> =
        groupEventFunction.takeEvent(groupId, eventId)
            .onSuccess { analytics.log(AnalyticsEvent.SwapTaken) }
            .onFailure { error ->
                if (error == SwapError.TakenBySomeoneElse) analytics.log(AnalyticsEvent.SwapTakeLost)
            }

    override suspend fun returnEvent(groupId: GroupId, eventId: EventId): Outcome<Unit, SwapError> =
        groupEventFunction.returnEvent(groupId, eventId)
            .onSuccess { analytics.log(AnalyticsEvent.SwapReturned) }

    override fun getEventsByGroup(
        groupId: GroupId,
        date: LocalDate,
        monthDelta: Int,
    ): Flow<List<GroupEvent>> = userRepository.loggedUserFlow.flatMapLatest { user ->
        val userId = user.id

        combine(
            userPrivateFirestore.observePreferences(userId).map { it.groupEventTypeColors },
            revokedGroupFirestore.observe(userId)
        ) { colors, revoked ->
            Pair(colors, revoked)
        }.flatMapLatest { (colors, revoked) ->
            revoked.find { it.id == groupId.value }?.let { revokedDoc ->
                val group = groupMapper.map(revokedDoc, colors)
                return@flatMapLatest eventsOf(group, emptyMap(), userId, date, monthDelta)
            }

            groupFirestore.observe(groupId).flatMapLatest { holder ->
                if (holder == null) flowOf(emptyList())
                else eventsOf(
                    holder,
                    userId,
                    colors,
                    date,
                    monthDelta
                )
            }

        }
    }

    override fun getEventsByUser(
        userId: UserId,
        date: LocalDate,
        monthDelta: Int,
    ): Flow<List<GroupEvent>> = userRepository.loggedUserFlow.flatMapLatest { viewer ->

        // Following the groups too: joining one, or an admin renaming a type, reaches the calendar
        // without a reload — the shifts are drawn from the group as much as from the events.
        getGroups().flatMapLatest { groups ->
            if (groups.isEmpty()) return@flatMapLatest flowOf(emptyList<GroupEvent>())

            // Combined, so a group answering from cache paints while another is on the wire.
            val perGroup: List<Flow<List<GroupEvent>>> = groups.map { group ->
                eventsOf(group, group.memberNames(), viewer.id, date, monthDelta)
            }

            combine(perGroup) { events ->
                events.toList().flatten().filter { it.assigneeId == userId || it.ownerId == userId }
            }
        }
    }

    override fun getSwapEvents(date: LocalDate, monthsAhead: Int): Flow<List<GroupEvent>> = flow {
        val viewer = userRepository.loggedUser?.id
        if (viewer == null) {
            emit(emptyList())
            return@flow
        }

        emitAll(
            getGroups().flatMapLatest { groups ->
                if (groups.isEmpty()) return@flatMapLatest flowOf(emptyList())

                val perGroup = groups.map { group ->
                    eventsBetween(
                        group = group,
                        memberNames = group.memberNames(),
                        viewer = viewer,
                        from = date,
                        until = date.plus(monthsAhead, DateTimeUnit.MONTH),
                    )
                }
                combine(perGroup) { events ->
                    events.toList().flatten().filter { it.date >= date }
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
    ): Flow<List<GroupEvent>> = eventsBetween(
        group = group,
        memberNames = memberNames,
        viewer = viewer,
        from = date.minus(monthDelta, DateTimeUnit.MONTH),
        until = date.plus(monthDelta, DateTimeUnit.MONTH),
    )

    private fun eventsBetween(
        group: Group,
        memberNames: Map<String, String>,
        viewer: UserId,
        from: LocalDate,
        until: LocalDate,
    ): Flow<List<GroupEvent>> = groupEventFirestore.get(
        group.id,
        from = from.toInstant(),
        until = until.toInstant(),
        assigneeId = viewer.takeIf { group.isRevoked },
    ).map { events ->
        events.mapNotNull { groupMapper.map(it, group, memberNames) }
    }

    private suspend fun typeColors(userId: UserId): Map<String, String> =
        userPrivateFirestore.fetchCachedPreferences(userId).valueOrNull()
            ?.groupEventTypeColors.orEmpty()

    companion object {
        private const val TAG = "GroupRepository"
    }
}
