package com.geoviksoft.turnia.core.data.user

import com.geoviksoft.turnia.core.data.datasource.firestore.doc.DocHolder
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.GroupDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.GroupMemberDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.InvitationDocument
import com.geoviksoft.turnia.core.data.datasource.firestore.doc.UserDocument
import com.geoviksoft.turnia.core.data.group.mappers.GroupMapper
import com.geoviksoft.turnia.core.data.user.mappers.UserDocumentMapper
import com.geoviksoft.turnia.core.domain.model.UserId
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
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

    /**
     * An admin's group save sends `members` back whole, as it read it: a member's photo survives only
     * if the document it is read into keeps the field.
     */
    @Test
    fun aGroupSaveWritesThePhotosItRead() {
        val serializer = MapSerializer(String.serializer(), GroupMemberDocument.serializer())
        val read = Json.decodeFromString(
            serializer,
            """{"bruno":{"name":"Bruno","username":"bruno","photoId":"p1"},"carla":{"name":"Carla","username":"carla"}}""",
        )

        val written = Json.decodeFromString(serializer, Json.encodeToString(serializer, read))

        assertEquals("p1", written.getValue("bruno").photoId)
        assertNull(written.getValue("carla").photoId)
    }

    private fun group(members: Map<String, GroupMemberDocument>) = GroupDocument(
        name = "Urgencias",
        memberUids = members.keys.toList(),
        members = members,
        invitation = InvitationDocument(code = "URG001"),
        updateAt = null,
    )
}
