package com.geoviksoft.turnia.core.domain.model

/**
 * Why offering or taking a shift did not happen.
 *
 * The lower half mirrors the codes `takeEvent` reserves in `firebase/functions/src/errors.ts`; the
 * upper half is what the client refuses before it writes anything.
 */
enum class SwapError {
    /** Only whoever covers a shift can offer it. */
    NotAssignee,

    /** The group's event type does not allow swapping. */
    NotSwappable,

    /** Taking a shift of a group the user does not belong to (3003). */
    NotMember,

    /** Taking a shift already assigned to oneself (3004). */
    OwnShift,

    /** The shift is gone: deleted while it was on offer (3005). */
    NotFound,

    /**
     * Somebody else got there first (3006). Two members tapping at once both see `onSwap == true`;
     * the transaction lets the earlier commit through and this is what the other one is told.
     */
    TakenBySomeoneElse,

    SaveFailed,
}
