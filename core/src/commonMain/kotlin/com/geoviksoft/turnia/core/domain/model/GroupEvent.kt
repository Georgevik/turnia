package com.geoviksoft.turnia.core.domain.model

import kotlinx.datetime.LocalDate

data class GroupEvent(
    val id: EventId,
    val groupId: GroupId,
    val groupName: String,
    val ownerId: UserId,
    val assigneeId: UserId,
    val assigneeName: String,
    val type: GroupEventType,
    val date: LocalDate,
    val onSwap: Boolean,
    val colorHex: String,
    val history: List<EventHistoryEntry>,
) {
    /**
     * Who holds the shift and who held it before them: the creator first, each transfer adding
     * whoever took it, each hand-back removing whoever gave it back. Mirrors `holderStack` in
     * `firebase/functions/src/events.ts`, which decides who a hand-back goes to.
     */
    val holders: List<EventHistoryEntry>
        get() = history.fold(emptyList()) { stack, entry ->
            if (entry.returned) stack.dropLast(1) else stack + entry
        }

    /** Whoever the current holder would give the shift back to, or null if it is still the creator's. */
    val previousHolder: EventHistoryEntry?
        get() = holders.takeIf { it.size > 1 }?.let { it[it.lastIndex - 1] }
}
