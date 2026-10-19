package com.geoviksoft.turnia.core.data.datasource.firestore

import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackData
import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.trackWrite
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.DocHolder
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.PersonalEventTypeDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.errors.GenericFirestoreError
import com.geoviksoft.turnia.core.data.datasource.firestore.sync.SharedListeners
import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.data.user.mappers.PersonalEventTypeDocMapper
import com.geoviksoft.turnia.core.domain.model.EventTypeId
import com.geoviksoft.turnia.core.domain.model.PersonalEventType
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.outcomeCatching
import com.geoviksoft.turnia.core.system.valueOrNull
import com.geoviksoft.turnia.core.system.toInstantOrNull
import com.geoviksoft.turnia.core.system.toTimestamp
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Interacts with Firestore: `users/{uid}/personalEventTypes`
 */
@OptIn(ExperimentalAtomicApi::class)
class PersonalEventTypesFirestore(
    private val firestore: FirebaseFirestore,
    private val personalEventTypeDocMapper: PersonalEventTypeDocMapper,
    private val userSyncFirestore: UserSyncFirestore,
    scope: CoroutineScope
) {

    private val listeners = SharedListeners<UserId, List<PersonalEventType>>(scope)

    fun observe(uid: UserId): Flow<List<PersonalEventType>> =
        listeners.shared(uid) { snapshots(uid) }

    private fun snapshots(uid: UserId): Flow<List<PersonalEventType>> = flow {
        val cachedTypes = queryEventTypes(uid, null, Source.CACHE)

        var known = cachedTypes
        emit(personalEventTypeDocMapper.map(known))

        emitAll(userSyncFirestore.observeWithPendingWrites(uid).mapNotNull { synced ->
            val serverCannotAnswer = !synced.confirmed
            if (serverCannotAnswer) {
                known = known.mergedWith(queryEventTypes(uid, null, Source.CACHE))
                return@mapNotNull personalEventTypeDocMapper.map(known)
            }

            val cacheUpdatedAt = known.mapNotNull { it.doc.updateAt.toInstantOrNull() }.maxOrNull()

            val serverUpdatedAt =
                synced.value.personalEventTypesUpdatedAt.toInstantOrNull()
                    ?: return@mapNotNull null

            val settled = cacheUpdatedAt != null && cacheUpdatedAt >= serverUpdatedAt
            if (settled) {
                return@mapNotNull personalEventTypeDocMapper.map(known)
            }

            val fromServer = outcomeCatching(TAG, { it }) {
                queryEventTypes(uid, cacheUpdatedAt?.toTimestamp(), Source.SERVER)
            }.valueOrNull()
                ?: outcomeCatching(TAG, { it }) { queryEventTypes(uid, null, Source.CACHE) }
                    .valueOrNull()
                ?: known

            known = known.mergedWith(fromServer)
            personalEventTypeDocMapper.map(known)
        })
    }.distinctUntilChanged().catch { throwable ->
        Logger.e(TAG, "Personal event types listener failed", throwable)
        emit(emptyList())
    }

    private fun List<DocHolder<PersonalEventTypeDocument>>.mergedWith(
        fresh: List<DocHolder<PersonalEventTypeDocument>>
    ): List<DocHolder<PersonalEventTypeDocument>> {
        val incoming = fresh.associateBy { it.id }.toMutableMap()
        return map { held -> incoming.remove(held.id) ?: held } + incoming.values
    }

    suspend fun set(
        uid: UserId, personalType: PersonalEventType
    ): Outcome<Unit, GenericFirestoreError> = outcomeCatching(TAG, { GenericFirestoreError(it) }) {
        val doc = personalEventTypeDocMapper.map(personalType)
        Logger.i(TAG, "Set personal type document")

        val batch = firestore.batch()
        batch.set(
            firestore.collection(PATH_PERSONAL_TYPES(uid.value)).document(personalType.id.value),
            doc,
        )
        val syncWrite = userSyncFirestore.writePersonalEventTypes(batch, uid)
        batch.commit()
        trackWrite(TAG, "setEventType")
        syncWrite.committed()
    }

    /** Whether [uid] has any personal type, deleted or not, from [source] alone: one read at most. */
    suspend fun hasAnyType(uid: UserId, source: Source): Outcome<Boolean, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            firestore.collection(PATH_PERSONAL_TYPES(uid.value))
                .limit(1)
                .get(source)
                .trackData(TAG, "hasAnyType($source)")
                .documents.isNotEmpty()
        }

    /** Every type in one commit: the shift setup never leaves an account with half its shifts. */
    suspend fun setAll(
        uid: UserId, types: List<PersonalEventType>
    ): Outcome<Unit, GenericFirestoreError> = outcomeCatching(TAG, { GenericFirestoreError(it) }) {
        Logger.i(TAG, "Set ${types.size} personal type documents")

        val batch = firestore.batch()
        types.forEach { type ->
            batch.set(
                firestore.collection(PATH_PERSONAL_TYPES(uid.value)).document(type.id.value),
                personalEventTypeDocMapper.map(type),
            )
        }
        val syncWrite = userSyncFirestore.writePersonalEventTypes(batch, uid)
        batch.commit()
        trackWrite(TAG, "setEventTypes", documents = types.size)
        syncWrite.committed()
    }

    suspend fun delete(uid: UserId, typeId: EventTypeId): Outcome<Unit, GenericFirestoreError> =
        outcomeCatching(TAG, { GenericFirestoreError(it) }) {
            Logger.i(TAG, "Delete personal type document")

            val batch = firestore.batch()
            batch.updateFields(
                firestore.collection(PATH_PERSONAL_TYPES(uid.value)).document(typeId.value)
            ) {
                PersonalEventTypeDocument.FIELD_IS_DELETED to true
                PersonalEventTypeDocument.FIELD_UPDATE_AT to Timestamp.ServerTimestamp
            }
            val syncWrite = userSyncFirestore.writePersonalEventTypes(batch, uid)
            batch.commit()
            trackWrite(TAG, "deleteEventType")
            syncWrite.committed()
        }

    private suspend fun queryEventTypes(
        uid: UserId, sinceUpdateAt: Timestamp?, source: Source
    ): List<DocHolder<PersonalEventTypeDocument>> {
        val collection = firestore.collection(PATH_PERSONAL_TYPES(uid.value))
        val query = if (sinceUpdateAt == null) collection else collection.where {
            PersonalEventTypeDocument.FIELD_UPDATE_AT greaterThan sinceUpdateAt
        }
        val snapshot = query.get(source).trackData(TAG, "eventTypes($source)")

        Logger.i(
            TAG,
            "Personal event types. Source: $source. " + "Amount: ${snapshot.documents.size}. " + "Changes: ${snapshot.documentChanges.size}"
        )

        return snapshot.documents.map { personalEventTypeDocMapper.map(it) }
    }

    companion object {
        private const val TAG = "PersonalEventTypesFirestore"
        private fun PATH_PERSONAL_TYPES(uid: String) = "users/${uid}/personalEventTypes"
    }
}
