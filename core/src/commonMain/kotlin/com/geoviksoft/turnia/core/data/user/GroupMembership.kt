package com.geoviksoft.turnia.core.data.user

import com.geoviksoft.turnia.core.data.datasource.firestore.GroupFirestore
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.mapError
import dev.gitlive.firebase.firestore.Source

/**
 * Whether an account is a member of any group, as the server says. The seam that lets the team
 * prompt's decision — offline above all — be tested without Firestore.
 */
interface GroupMembership {

    /** A failure means no answer, which is not the same as no group. */
    suspend fun serverHasAnyGroup(uid: UserId): Outcome<Boolean, Unit>
}

class FirestoreGroupMembership(private val groupFirestore: GroupFirestore) : GroupMembership {

    override suspend fun serverHasAnyGroup(uid: UserId): Outcome<Boolean, Unit> =
        groupFirestore.hasAnyGroup(uid, Source.SERVER).mapError { }
}
