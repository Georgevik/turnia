package com.georgevik.turnia.core.data.datasource.firestore.mappers

import com.georgevik.turnia.core.data.datasource.firestore.doc.Tier
import com.georgevik.turnia.core.data.datasource.firestore.doc.UserDocument
import com.georgevik.turnia.core.domain.model.Membership
import com.georgevik.turnia.core.domain.model.User
import com.georgevik.turnia.core.domain.model.UserProfile
import com.georgevik.turnia.core.system.MOCK_MY_ID
import dev.gitlive.firebase.auth.FirebaseUser

class UserDocumentMapper {

    fun map(firebaseUser: FirebaseUser) = User(
        uid = MOCK_MY_ID, // TODO Remove, use firebaseUser.uid
        firebaseUid = firebaseUser.uid,
        email = firebaseUser.email.orEmpty(),
        displayName = firebaseUser.displayName.orEmpty(),
        membership = Membership.FREE,
    )

    fun map(firebaseUser: FirebaseUser, profile: UserProfile?) =
        if (profile == null) map(firebaseUser)
        else User(
            uid = MOCK_MY_ID, // TODO Remove
            firebaseUid = firebaseUser.uid,
            email = profile.email,
            displayName = profile.name,
            membership = profile.membership,
        )

    fun map(uid: String, document: UserDocument) = UserProfile(
        id = uid,
        name = document.name,
        email = document.email,
        membership = if (document.subscription.tier == Tier.FREE) Membership.PREMIUM else Membership.FREE,
    )


}
