package com.geoviksoft.turnia.ui.components.daydetail.model

/** Why offering or taking a shift did not work, for the sheet to turn into a sentence. */
enum class DaySwapMessage {
    NotAssignee,
    NotSwappable,
    NotMember,
    OwnShift,
    NotFound,
    TakenBySomeoneElse,
    NothingToReturn,
    PreviousHolderLeft,
    SaveFailed,
}
