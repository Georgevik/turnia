package com.geoviksoft.turnia.core.data.user

import com.geoviksoft.turnia.core.data.datasource.firestore.doc.DocHolder
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.GroupDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.GroupEditDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.GroupMemberDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.InvitationDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.UserDocument
import com.geoviksoft.turnia.core.data.group.mappers.GroupMapper
import com.geoviksoft.turnia.core.data.user.mappers.UserDocumentMapper
import com.geoviksoft.turnia.core.domain.model.UserId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProfilePhotoMappingTest {

    @Test
    fun aProfileCarriesItsPhoto() {
        val profile = UserDocumentMapper().map(UserId("bruno"), UserDocument(name = "Bruno", photoId = "p1"))

        assertEquals("p1", profile.photoId)
    }

    @Test
    fun aProfileWithoutAPhotoHasNone() {
        assertNull(UserDocumentMapper().map(UserId("bruno"), UserDocument(name = "Bruno")).photoId)
    }

    @Test
    fun aGroupMemberCarriesTheCopiedPhoto() {
        val group = GroupMapper().map(
            DocHolder("urgencias", group(members = mapOf("bruno" to GroupMemberDocument("Bruno", "bruno", "p1")))),
            viewer = UserId("alice"),
            colors = emptyMap(),
        )

        assertEquals("p1", group.members.single().photoId)
    }

    /** A group save merges what an admin edits and nothing else, so it can never wipe a member's photo. */
    @Test
    fun aGroupSaveSendsNoMembership() {
        val edit = GroupEditDocument.of(group(members = mapOf("bruno" to GroupMemberDocument("Bruno", "bruno", "p1"))))
            .copy(updateAt = null)

        val sent = Json.encodeToJsonElement(GroupEditDocument.serializer(), edit).jsonObject.keys

        assertEquals(setOf("name", "color", "groupEventTypes", "invitation", "updateAt"), sent)
    }

    private fun group(members: Map<String, GroupMemberDocument>) = GroupDocument(
        name = "Urgencias",
        memberUids = members.keys.toList(),
        members = members,
        invitation = InvitationDocument(code = "URG001"),
        updateAt = null,
    )
}
