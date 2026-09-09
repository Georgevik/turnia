package com.geoviksoft.turnia.core.data.group

import com.geoviksoft.turnia.core.domain.model.Group
import com.geoviksoft.turnia.core.domain.model.GroupId
import com.geoviksoft.turnia.core.domain.model.NewGroup

/**
 * Builds the group a [NewGroup] becomes the moment it is written: it gains an [GroupId], its
 * creator as its only member, and the admin rights that come with having created it.
 */
class GroupFactory {

    fun create(group: NewGroup, id: GroupId, invitationCode: String) = Group(
        id = id,
        name = group.name,
        color = group.color,
        types = group.types,
        // The roster is the group document's, and it is filled in as the group is written; the
        // count is what a member is: one, the creator.
        members = emptyList(),
        memberCount = 1,
        invitationCode = invitationCode,
        autoApprove = group.autoApprove,
        membersCanSeeCode = group.membersCanSeeCode,
        isAdmin = true,
    )
}
