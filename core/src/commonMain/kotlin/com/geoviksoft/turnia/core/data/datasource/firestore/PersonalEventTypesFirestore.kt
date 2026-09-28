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
import com.geoviksoft.turnia.core.system.toInstantOrNull
import com.geoviksoft.turnia.core.system.toTimestamp
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
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

        emitAll(userSyncFirestore.observe(uid).mapNotNull { sync ->
            val cacheUpdatedAt = known.mapNotNull { it.doc.updateAt.toInstantOrNull() }.maxOrNull()

            val serverUpdatedAt =
                sync.personalEventTypesUpdatedAt.toInstantOrNull() ?: return@mapNotNull null

            val settled = cacheUpdatedAt != null && cacheUpdatedAt >= serverUpdatedAt
            if (settled) {
                return@mapNotNull personalEventTypeDocMapper.map(known)
            }

            val changed = queryEventTypes(
                uid,
                cacheUpdatedAt?.toTimestamp(),
                Source.SERVER
            ).associateBy { it.id }.toMutableMap()

            val merged = known.map { cached -> changed.remove(cached.id) ?: cached }
            known = merged + changed.values
            personalEventTypeDocMapper.map(known)
        })
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
        val since = sinceUpdateAt ?: Timestamp(0, 0)
        val snapshot = firestore.collection(PATH_PERSONAL_TYPES(uid.value)).where {
            PersonalEventTypeDocument.FIELD_UPDATE_AT greaterThan since
        }.get(source).trackData(TAG, "eventTypes($source)")

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
