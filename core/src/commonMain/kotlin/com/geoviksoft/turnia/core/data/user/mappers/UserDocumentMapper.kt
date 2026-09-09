package com.geoviksoft.turnia.core.data.user.mappers

import com.geoviksoft.turnia.core.data.datasource.firestore.doc.SubscriptionDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.Tier
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.UserDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.UsernameDocument
import com.geoviksoft.turnia.core.domain.model.Membership
import com.geoviksoft.turnia.core.domain.model.User
import com.geoviksoft.turnia.core.domain.model.UserId
import com.geoviksoft.turnia.core.domain.model.UserProfile
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.firestore.DocumentSnapshot

class UserDocumentMapper {

    fun map(firebaseUser: FirebaseUser) = User(
        id = UserId(firebaseUser.uid),
        email = firebaseUser.email.orEmpty(),
        displayName = firebaseUser.displayName.orEmpty(),
        username = "",
        membership = Membership.FREE,
    )

    fun map(
        firebaseUser: FirebaseUser,
        profile: UserProfile?,
        subscription: SubscriptionDocument?,
    ) = User(
        id = UserId(firebaseUser.uid),
        // Auth already knows the email; the profile document no longer carries it.
        email = firebaseUser.email.orEmpty(),
        displayName = profile?.name ?: firebaseUser.displayName.orEmpty(),
        username = profile?.username.orEmpty(),
        membership = subscription.toMembership(),
    )

    /** A reservation carries the least there is to know about a user nobody can read yet. */
    fun map(document: UsernameDocument) = UserProfile(
        id = UserId(document.uid),
        name = document.name,
        username = document.username,
    )

    fun map(snapshot: DocumentSnapshot): UserProfile =
        map(UserId(snapshot.reference.id), snapshot.data(UserDocument.serializer()))

    fun map(uid: UserId, document: UserDocument) = UserProfile(
        id = uid,
        name = document.name,
        username = document.username,
    )

    /** No subscription document means the user has never bought anything: free tier. */
    private fun SubscriptionDocument?.toMembership(): Membership =
        if (this?.tier == Tier.PREMIUM) Membership.PREMIUM else Membership.FREE
}
