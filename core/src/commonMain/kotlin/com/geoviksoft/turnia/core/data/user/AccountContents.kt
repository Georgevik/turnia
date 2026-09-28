package com.geoviksoft.turnia.core.data.user

import com.geoviksoft.turnia.core.data.datasource.firestore.GroupFirestore
import com.geoviksoft.turnia.core.data.datasource.firestore.PersonalEventTypesFirestore
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.flatMap
import com.geoviksoft.turnia.core.system.mapError
import com.geoviksoft.turnia.core.system.toSuccess
import com.geoviksoft.turnia.core.system.valueOrNull
import dev.gitlive.firebase.firestore.Source

/**
 * Whether an account already has something to reuse: a personal type or a group.
 *
 * An interface with one implementation on purpose: it is the seam that lets the shift setup's
 * decision — the offline case above all — be tested without Firestore.
 */
interface AccountContents {

    /** From the device's cache only: free, and `false` when the cache simply knows nothing yet. */
    suspend fun cachedHasAny(uid: UserId): Boolean

    /** From the server: a failure means no answer, which is not the same as an empty account. */
    suspend fun serverHasAny(uid: UserId): Outcome<Boolean, Unit>
}

class FirestoreAccountContents(
    private val personalEventTypesFirestore: PersonalEventTypesFirestore,
    private val groupFirestore: GroupFirestore,
) : AccountContents {

    override suspend fun cachedHasAny(uid: UserId): Boolean =
        personalEventTypesFirestore.hasAnyType(uid, Source.CACHE).valueOrNull() == true ||
            groupFirestore.hasAnyGroup(uid, Source.CACHE).valueOrNull() == true

    override suspend fun serverHasAny(uid: UserId): Outcome<Boolean, Unit> =
        personalEventTypesFirestore.hasAnyType(uid, Source.SERVER).mapError { }
            .flatMap { hasType ->
                if (hasType) true.toSuccess()
                else groupFirestore.hasAnyGroup(uid, Source.SERVER).mapError { }
            }
}
