package com.georgevik.turnia.core.data.group

import com.georgevik.turnia.core.data.datasource.firestore.GroupEventFirestore
import com.georgevik.turnia.core.data.datasource.firestore.GroupFirestore
import com.georgevik.turnia.core.data.datasource.firestore.UserPathFirestore
import com.georgevik.turnia.core.data.datasource.firestore.doc.DocHolder
import com.georgevik.turnia.core.data.datasource.firestore.doc.GroupDocument
import com.georgevik.turnia.core.data.datasource.firestore.mappers.GroupMapper
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.EventId
import com.georgevik.turnia.core.domain.model.EventTypeId
import com.georgevik.turnia.core.domain.model.Group
import com.georgevik.turnia.core.domain.model.GroupError
import com.georgevik.turnia.core.domain.model.GroupEvent
import com.georgevik.turnia.core.domain.model.GroupId
import com.georgevik.turnia.core.domain.model.UserId
import com.georgevik.turnia.core.domain.repository.GroupRepository
import com.georgevik.turnia.core.domain.repository.UserRepository
import com.georgevik.turnia.core.system.Outcome
import com.georgevik.turnia.core.system.createId
import com.georgevik.turnia.core.system.createInvitationCode
import com.georgevik.turnia.core.system.errorOrNull
import com.georgevik.turnia.core.system.mapError
import com.georgevik.turnia.core.system.toFailure
import com.georgevik.turnia.core.system.toInstant
import com.georgevik.turnia.core.system.toSuccess
import com.georgevik.turnia.core.system.valueOrNull
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

class GroupRepositoryImpl(
    private val userRepository: UserRepository,
    private val groupFirestore: GroupFirestore,
    private val groupEventFirestore: GroupEventFirestore,
    private val userPathFirestore: UserPathFirestore,
    private val groupMapper: GroupMapper,
) : GroupRepository {

    override suspend fun getGroups(): List<Group> {
        val userId = userRepository.loggedUser?.id ?: return emptyList()

        return groupFirestore.getMyGroups(userId).valueOrNull().orEmpty()
            .map { groupMapper.map(it, userId, typeColors(userId)) }
    }

    override suspend fun getGroup(groupId: GroupId): Outcome<Group, GroupError> {
        val userId = userRepository.loggedUser?.id ?: return GroupError.NotFound.toFailure()
        val holder = groupFirestore.get(groupId).valueOrNull() ?: return GroupError.NotFound.toFailure()

        return groupMapper.map(holder, userId, typeColors(userId)).toSuccess()
    }

    override suspend fun saveGroup(group: Group): Outcome<Group, GroupError> {
        val userId = userRepository.loggedUser?.id ?: return GroupError.NotFound.toFailure()
        val isNew = group.id.value.isBlank()

        val current = if (isNew) null else groupFirestore.get(group.id).valueOrNull()
        val groupId = if (isNew) GroupId(createId()) else group.id
        val memberUids = current?.doc?.memberUids ?: listOf(userId.value)
        val adminUids = current?.doc?.adminUids ?: listOf(userId.value)

        val invitationCode = group.invitationCode ?: createInvitationCode()
        val document = groupMapper.map(group.copy(invitationCode = invitationCode), memberUids, adminUids)
            .copy(members = current?.doc?.members.orEmpty())

        groupFirestore.save(groupId, document).errorOrNull()?.let { error ->
            Logger.e(TAG, "Failed to save group: $error")
            return GroupError.NotFound.toFailure()
        }

        return group.copy(id = groupId, invitationCode = invitationCode, isAdmin = userId.value in adminUids)
            .toSuccess()
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

    override suspend fun getEventsByGroup(
        groupId: GroupId,
        date: LocalDate,
        monthDelta: Int
    ): Outcome<List<GroupEvent>, Unit> {
        val userId = userRepository.loggedUser?.id ?: return Unit.toFailure()
        val holder = groupFirestore.get(groupId).valueOrNull() ?: return Unit.toFailure()

        return eventsOf(holder, userId, date, monthDelta).toSuccess()
    }

    override suspend fun getEventsByUser(
        userId: UserId,
        date: LocalDate,
        monthDelta: Int
    ): Result<List<GroupEvent>> {
        val viewer = userRepository.loggedUser?.id ?: return Result.success(emptyList())

        val events = groupFirestore.getMyGroups(viewer).valueOrNull().orEmpty()
            .flatMap { holder -> eventsOf(holder, viewer, date, monthDelta) }
            .filter { it.assigneeId == userId || it.ownerId == userId }

        return Result.success(events)
    }

    private suspend fun eventsOf(
        holder: DocHolder<GroupDocument>,
        viewer: UserId,
        date: LocalDate,
        monthDelta: Int
    ): List<GroupEvent> {
        val group = groupMapper.map(holder, viewer, typeColors(viewer))
        val memberNames = holder.doc.members.mapValues { (_, member) -> member.name }

        val documents = groupEventFirestore.get(
            group.id,
            from = date.minus(monthDelta, DateTimeUnit.MONTH).toInstant(),
            until = date.plus(monthDelta, DateTimeUnit.MONTH).toInstant(),
        ).valueOrNull().orEmpty()

        return documents.mapNotNull { groupMapper.map(it, group, memberNames) }
    }

    private suspend fun typeColors(userId: UserId): Map<String, String> =
        userPathFirestore.getUserDocument(userId).valueOrNull()?.groupEventTypeColors.orEmpty()

    companion object {
        private const val TAG = "GroupRepository"
    }
}
