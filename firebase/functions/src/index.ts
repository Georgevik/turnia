import { setGlobalOptions } from "firebase-functions/v2";
import { initializeApp } from "firebase-admin/app";

initializeApp();

// Where Firestore is. A trigger has no choice — it must live in the database's region — and a
// callable that reads Firestore on every request should not be crossing an ocean to do it.
// The client has to name the same region: `Firebase.functions(...)` defaults to us-central1.
setGlobalOptions({ region: "europe-southwest1" });

// Group membership: request to join (validates the invitation) and the admin's answer.
export { requestToJoinGroup, acceptJoinRequest, rejectJoinRequest } from "./invitations";

// Withdrawing membership: leaving yourself, or an admin removing someone. Whoever still holds
// events keeps read access to their own through `revokedUids`.
export { leaveGroup, removeMember, deleteGroup } from "./membership";

// Takes an event offered for swap: verifies `onSwap` and reassigns it in a transaction. A taker
// who can no longer cover it gives it back to the previous holder with `returnEvent`.
export { takeEvent, returnEvent } from "./events";

// Renames a user everywhere their name is copied: the client may not write it itself.
export { updateProfile } from "./users";

// Deletes the caller's account by anonymizing it: the uid stays everywhere it is referenced, and
// everything that says who it was goes, along with the Auth user.
export { deleteAccount } from "./account";

// Push. `onEventPutOnSwap` and `onCalendarShared` react to writes a client makes directly; the
// join-request notifications are sent inline by the callables in `invitations.ts`, which already
// hold everything they need.
export { onEventPutOnSwap, onCalendarShared } from "./notifications";

// On-demand aggregation of another user's full calendar (cross-group), and the per-holder marker
// that tells whoever it is shared with when to ask again.
export { getSharedCalendar, onGroupEventWrittenMarkHolders } from "./sharedCalendar";
