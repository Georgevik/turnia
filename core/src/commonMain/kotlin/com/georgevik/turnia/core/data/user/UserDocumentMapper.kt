package com.georgevik.turnia.core.data.user

import com.georgevik.turnia.core.data.user.datasource.PersonalEventTypeDocument
import com.georgevik.turnia.core.data.user.datasource.Tier
import com.georgevik.turnia.core.data.user.datasource.UserDocument
import com.georgevik.turnia.core.domain.model.Membership
import com.georgevik.turnia.core.domain.model.PersonalEventType
import com.georgevik.turnia.core.domain.model.User
import com.georgevik.turnia.core.domain.model.UserProfile
import com.georgevik.turnia.core.system.MOCK_MY_ID
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.firestore.DocumentSnapshot

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

    fun mapToPersonalEventType(snapshot: DocumentSnapshot): PersonalEventType {
        val doc = snapshot.data(PersonalEventTypeDocument.serializer())

        return PersonalEventType(
            id = snapshot.reference.id,
            name = doc.name,
            color = doc.color,
            acronym = doc.acronym,
            description = doc.description,
            startTime = doc.startTime,
            endTime = doc.endTime,
        )
    }
}
