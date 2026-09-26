package com.geoviksoft.turnia.demo

import com.geoviksoft.turnia.core.domain.analytics.Analytics
import com.geoviksoft.turnia.core.domain.analytics.AnalyticsEvent
import com.geoviksoft.turnia.core.domain.model.DeleteAccountError
import com.geoviksoft.turnia.core.domain.model.EmailAuthError
import com.geoviksoft.turnia.core.domain.model.EventHistoryEntry
import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.core.domain.model.EventTypeId
import com.geoviksoft.turnia.core.domain.model.FeatureFlags
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
import com.geoviksoft.turnia.core.domain.model.PersonalEventType
import com.geoviksoft.turnia.core.domain.model.PersonalOneOffEvent
import com.geoviksoft.turnia.core.domain.model.PersonalTypedEvent
import com.geoviksoft.turnia.core.domain.model.SharedCalendar
import com.geoviksoft.turnia.core.domain.model.SharedCalendarError
import com.geoviksoft.turnia.core.domain.model.SwapError
import com.geoviksoft.turnia.core.domain.model.User
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.domain.model.UserProfile
import com.geoviksoft.turnia.core.domain.model.UserSession
import com.geoviksoft.turnia.core.domain.model.UsernameError
import com.geoviksoft.turnia.core.domain.repository.AppConfigRepository
import com.geoviksoft.turnia.core.domain.repository.GroupRepository
import com.geoviksoft.turnia.core.domain.repository.PersonalEventRepository
import com.geoviksoft.turnia.core.domain.repository.SharedCalendarRepository
import com.geoviksoft.turnia.core.domain.repository.UserRepository
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.toFailure
import com.geoviksoft.turnia.core.system.toSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minusMonth
import kotlinx.datetime.plusMonth
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * The repositories the demo runs on: the same contracts as the real ones, answered from [DemoWorld]
 * in memory. Writes change that memory, so asking for a swap or covering a shift behaves as it
 * would for real until the app is restarted — and nothing ever reaches Firebase.
 */
internal class DemoUserRepository : UserRepository {

    private val session = MutableStateFlow<UserSession>(UserSession.Authenticated(DemoPeople.me))
    private val sharedByMe = MutableStateFlow(listOf(DemoPeople.elena.profile, DemoPeople.javier.profile))
    private val hiddenSharedCalendars = MutableStateFlow(emptySet<UserId>())
    private val notifications = MutableStateFlow(true)

    override val loggedUser: User get() = DemoPeople.me
    override val userSession: StateFlow<UserSession> = session.asStateFlow()
    override val loggedUserFlow: Flow<User> =
        session.filterIsInstance<UserSession.Authenticated>().map { it.user }

    // Never reached: the demo starts signed in, so there is no sign-in screen to call these from.
    override suspend fun signInWithEmail(email: String, password: String): Outcome<Unit, EmailAuthError> =
        Unit.toSuccess()

    override suspend fun createAccountWithEmail(
        name: String,
        email: String,
        password: String,
    ): Outcome<Unit, EmailAuthError> =
        Unit.toSuccess()

    override suspend fun sendPasswordReset(email: String): Outcome<Unit, EmailAuthError> = Unit.toSuccess()

    override suspend fun signOut() = Unit

    // Like signing out, a no-op: the demo account is made up, and there is nobody to sign in as next.
    override suspend fun deleteAccount(): Outcome<Unit, DeleteAccountError> = Unit.toSuccess()

    override fun getCalendarsSharedWithMe(): Flow<Outcome<List<UserProfile>, Unit>> =
        flowOf(listOf(DemoPeople.javier.profile, DemoPeople.marta.profile).toSuccess())

    override fun getHiddenSharedCalendars(): Flow<Set<UserId>> = hiddenSharedCalendars

    override suspend fun hideSharedCalendar(userId: UserId): Outcome<Unit, Unit> {
        hiddenSharedCalendars.update { it + userId }
        return Unit.toSuccess()
    }

    override suspend fun unhideSharedCalendar(userId: UserId): Outcome<Unit, Unit> {
        hiddenSharedCalendars.update { it - userId }
        return Unit.toSuccess()
    }

    override suspend fun updateProfile(name: String, username: String): Outcome<Unit, UsernameError> =
        Unit.toSuccess()

    override suspend fun updateAvatar(animalIconId: String?, backgroundColor: String?) =
        Unit.toSuccess()

    override suspend fun enableShowAds(): Outcome<Unit, Unit> = Unit.toSuccess()

    override suspend fun searchUsers(prefix: String): Outcome<List<UserProfile>, Unit> =
        DemoPeople.all.filter { it.username.startsWith(prefix.lowercase()) && it != DemoPeople.lucia }
            .map { it.profile }
            .toSuccess()

    override suspend fun getProfiles(userIds: List<UserId>): Outcome<List<UserProfile>, Unit> =
        DemoPeople.all.filter { it.id in userIds }.map { it.profile }.toSuccess()

    override suspend fun getCalendarSharedWith(): Outcome<List<UserProfile>, Unit> =
        sharedByMe.value.toSuccess()

    override suspend fun grantCalendarAccess(userId: UserId): Outcome<Unit, Unit> {
        DemoPeople.all.firstOrNull { it.id == userId }?.let { person ->
            sharedByMe.update { (it + person.profile).distinct() }
        }
        return Unit.toSuccess()
    }

    override suspend fun revokeCalendarAccess(userId: UserId): Outcome<Unit, Unit> {
        sharedByMe.update { shared -> shared.filterNot { it.id == userId } }
        return Unit.toSuccess()
    }

    override val notificationsEnabled: StateFlow<Boolean> = notifications.asStateFlow()

    override suspend fun registerFcmToken(uid: UserId) = Unit

    override suspend fun unregisterFcmToken(uid: UserId) = Unit

    override suspend fun setNotificationsEnabled(uid: UserId, enabled: Boolean): Outcome<Unit, Unit> {
        notifications.value = enabled
        return Unit.toSuccess()
    }
}

internal class DemoGroupRepository(private val world: DemoWorld) : GroupRepository {

    private val me = DemoPeople.me
    private val groups = MutableStateFlow(world.groups)
    private val events = MutableStateFlow(world.groupEvents)
    private val pending = MutableStateFlow<List<GroupEventType>>(emptyList())

    override fun getGroups(): Flow<List<Group>> = groups

    override fun createInvitationCode(): String = "DEMO42"

    override suspend fun addEvent(event: GroupEvent) {
        val name = DemoPeople.all.first { it.id == event.assigneeId }.name
        events.update {
            it + event.copy(
                assigneeName = name,
                history = listOf(EventHistoryEntry(event.ownerId, name)),
            )
        }
    }

    override suspend fun deleteEvent(
        groupId: GroupId,
        eventId: EventId,
        eventDate: LocalDate,
        ownerId: UserId,
        assigneeId: UserId,
    ): Outcome<Unit, Unit> {
        events.update { all -> all.filterNot { it.id == eventId } }
        return Unit.toSuccess()
    }

    override suspend fun setOnSwap(
        groupId: GroupId,
        eventId: EventId,
        eventDate: LocalDate,
        assigneeId: UserId,
        swappable: Boolean,
        onSwap: Boolean,
    ): Outcome<Unit, SwapError> {
        events.update { all -> all.map { if (it.id == eventId) it.copy(onSwap = onSwap) else it } }
        return Unit.toSuccess()
    }

    override suspend fun takeEvent(groupId: GroupId, eventId: EventId): Outcome<Unit, SwapError> {
        val event = events.value.firstOrNull { it.id == eventId } ?: return SwapError.NotFound.toFailure()
        if (!event.onSwap) return SwapError.TakenBySomeoneElse.toFailure()

        val name = me.displayName.orEmpty()
        events.update { all ->
            all.map {
                if (it.id != eventId) it
                else it.copy(
                    assigneeId = me.id,
                    assigneeName = name,
                    onSwap = false,
                    history = it.history + EventHistoryEntry(me.id, name),
                )
            }
        }
        return Unit.toSuccess()
    }

    override suspend fun returnEvent(groupId: GroupId, eventId: EventId): Outcome<Unit, SwapError> {
        val event = events.value.firstOrNull { it.id == eventId } ?: return SwapError.NotFound.toFailure()
        val previous = event.previousHolder ?: return SwapError.NothingToReturn.toFailure()

        events.update { all ->
            all.map {
                if (it.id != eventId) it
                else it.copy(
                    assigneeId = previous.userId,
                    assigneeName = previous.userName,
                    onSwap = true,
                    history = it.history + previous.copy(returned = true),
                )
            }
        }
        return Unit.toSuccess()
    }

    override fun getEventsByGroup(
        groupId: GroupId,
        date: LocalDate,
        monthDelta: Int,
    ): Flow<List<GroupEvent>> = events.map { all ->
        all.filter { it.groupId == groupId && it.date in window(date, monthDelta) }
    }

    override fun getEventsByUser(
        userId: UserId,
        date: LocalDate,
        monthDelta: Int,
    ): Flow<List<GroupEvent>> = events.map { all ->
        all.filter {
            (it.assigneeId == userId || it.ownerId == userId) && it.date in window(date, monthDelta)
        }
    }

    override fun getSwapEvents(date: LocalDate, monthsAhead: Int): Flow<List<GroupEvent>> =
        events.map { all ->
            all.filter { it.date >= date && it.date <= date.plus(monthsAhead, DateTimeUnit.MONTH) }
        }

    override suspend fun getGroup(groupId: GroupId): Outcome<Group, GroupError> =
        groups.value.firstOrNull { it.id == groupId }?.toSuccess() ?: GroupError.NotFound.toFailure()

    override suspend fun createGroup(group: NewGroup): Outcome<Group, GroupError> =
        GroupError.SaveFailed.toFailure()

    override suspend fun updateGroup(group: Group): Outcome<Group, GroupError> {
        groups.update { all -> all.map { if (it.id == group.id) group else it } }
        return group.toSuccess()
    }

    override fun observeGroup(groupId: GroupId): Flow<Outcome<Group, GroupError>> =
        groups.map { all ->
            all.firstOrNull { it.id == groupId }?.toSuccess() ?: GroupError.NotFound.toFailure()
        }

    /** One person waiting at the door of the group Lucía runs, so the admin side has something. */
    override fun observeJoinRequests(groupId: GroupId): Flow<List<JoinRequest>> = flowOf(
        if (groupId == world.urgencias) {
            val irene = DemoPeople.irene
            listOf(JoinRequest(irene.id, irene.name, irene.username))
        } else {
            emptyList()
        }
    )

    override suspend fun acceptJoinRequest(groupId: GroupId, userId: UserId): Outcome<Unit, GroupError> =
        Unit.toSuccess()

    override suspend fun requestToJoinGroup(code: String): Outcome<JoinGroupStatus, JoinGroupError> =
        JoinGroupError.CodeNotFound.toFailure()

    override suspend fun rejectJoinRequest(groupId: GroupId, userId: UserId): Outcome<Unit, GroupError> =
        Unit.toSuccess()

    override suspend fun getMyJoinRequests(): Flow<List<MyJoinRequest>> = flowOf(emptyList())

    override suspend fun leaveGroup(groupId: GroupId): Outcome<Unit, GroupError> =
        GroupError.SaveFailed.toFailure()

    override suspend fun removeMember(groupId: GroupId, userId: UserId): Outcome<Unit, GroupError> =
        GroupError.SaveFailed.toFailure()

    override suspend fun deleteGroup(groupId: GroupId): Outcome<Unit, GroupError> =
        GroupError.SaveFailed.toFailure()

    override suspend fun saveEventType(groupId: GroupId, type: GroupEventType): Outcome<Unit, GroupError> =
        Unit.toSuccess()

    override suspend fun saveTypeColor(groupId: GroupId, typeId: EventTypeId, color: String): Result<Unit> =
        Result.success(Unit)

    override val pendingEventTypes: StateFlow<List<GroupEventType>> = pending.asStateFlow()

    override fun setPendingEventType(type: GroupEventType) =
        pending.update { types -> types.filterNot { it.id == type.id } + type }

    override fun consumePendingEventTypes(): List<GroupEventType> =
        pending.value.also { pending.value = emptyList() }

    private fun window(date: LocalDate, monthDelta: Int) =
        date.minus(monthDelta, DateTimeUnit.MONTH)..date.plus(monthDelta, DateTimeUnit.MONTH)
}

internal class DemoPersonalEventRepository(world: DemoWorld) : PersonalEventRepository {

    private val types = MutableStateFlow(world.personalTypes)
    private val events = MutableStateFlow(world.personalEvents)
    private val oneOffEvents = MutableStateFlow(world.personalOneOffEvents)

    override fun getMyEventTypes(includeDeleted: Boolean): Flow<List<PersonalEventType>> =
        types.map { all -> all.filter { includeDeleted || !it.isDeleted } }

    override suspend fun addEvent(event: PersonalTypedEvent) = events.update { it + event }

    override suspend fun deleteEvent(eventId: EventId, eventDate: LocalDate) =
        events.update { all -> all.filterNot { it.id == eventId } }

    override suspend fun saveNotes(eventId: EventId, eventDate: LocalDate, notes: String?): Outcome<Unit, Unit> {
        events.update { all -> all.map { if (it.id == eventId) it.copy(notes = notes) else it } }
        return Unit.toSuccess()
    }

    override suspend fun saveEventType(type: PersonalEventType): Outcome<Unit, Unit> {
        types.update { all -> all.filterNot { it.id == type.id } + type }
        return Unit.toSuccess()
    }

    override suspend fun deleteEventType(typeId: EventTypeId): Outcome<Unit, Unit> {
        types.update { all -> all.map { if (it.id == typeId) it.copy(isDeleted = true) else it } }
        return Unit.toSuccess()
    }

    override fun getEvents(uid: UserId, date: LocalDate, monthDelta: Int): Flow<List<PersonalTypedEvent>> =
        events.map { all ->
            val window = date.minus(monthDelta, DateTimeUnit.MONTH)..date.plus(monthDelta, DateTimeUnit.MONTH)
            all.filter { it.date in window }
        }

    override fun getOneOffEvents(uid: UserId, date: LocalDate, monthDelta: Int): Flow<List<PersonalOneOffEvent>> =
        oneOffEvents.map { all ->
            val from = date.minus(monthDelta, DateTimeUnit.MONTH)
            val until = date.plus(monthDelta, DateTimeUnit.MONTH)
            all.filter { it.start.date <= until && it.end.date >= from }
        }

    override suspend fun addOneOffEvent(event: PersonalOneOffEvent): Outcome<Unit, Unit> {
        oneOffEvents.update { it + event }
        return Unit.toSuccess()
    }

    override suspend fun updateOneOffEvent(
        previous: PersonalOneOffEvent,
        event: PersonalOneOffEvent,
    ): Outcome<Unit, Unit> {
        oneOffEvents.update { all -> all.map { if (it.id == event.id) event else it } }
        return Unit.toSuccess()
    }

    override suspend fun deleteOneOffEvent(event: PersonalOneOffEvent): Outcome<Unit, Unit> {
        oneOffEvents.update { all -> all.filterNot { it.id == event.id } }
        return Unit.toSuccess()
    }
}

/** A colleague's calendar is their shifts, whichever group they are in; they keep no personal ones here. */
internal class DemoSharedCalendarRepository(private val world: DemoWorld) : SharedCalendarRepository {

    override fun sharedCalendar(
        ownerId: UserId,
        month: YearMonth,
    ): Flow<Outcome<SharedCalendar, SharedCalendarError>> {
        val from = month.minusMonth().firstDay
        val to = month.plusMonth().lastDay
        return flowOf(
            SharedCalendar(
                groupEvents = world.groupEvents.filter { it.assigneeId == ownerId && it.date in from..to },
                personalEvents = emptyList(),
            ).toSuccess()
        )
    }

}

internal object DemoAnalytics : Analytics {
    override fun log(event: AnalyticsEvent) = Unit

    override fun setUser(userId: UserId?) = Unit
}

internal class DemoAppConfigRepository : AppConfigRepository {
    // No ads in the demo: it exists for store screenshots.
    private val flags = MutableStateFlow(
        FeatureFlags(
            minActionsToEnableAds = -1,
            invitationCodeLength = 6,
            enableSubscription = false,
            supportEmail = "geoviksoft@gmail.com",
        )
    )

    override val featureFlags: StateFlow<FeatureFlags> = flags.asStateFlow()

    override suspend fun refreshFeatureFlags(): FeatureFlags = flags.value
    override suspend fun isOnboardingSeen(): Boolean = false
    override suspend fun setOnboardingSeen(seen: Boolean) = Unit
}
