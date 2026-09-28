package com.geoviksoft.turnia.core.domain.model

/** Whether a move to a group carried the tapped event alone or every event of its type. [value] is what analytics carries. */
enum class MoveScope(val value: String) {
    One("one"),
    All("all"),
}
