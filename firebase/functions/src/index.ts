import { initializeApp } from "firebase-admin/app";

initializeApp();

// Group membership: request to join (validates the invitation) and admin acceptance.
export { requestToJoinGroup, acceptJoinRequest } from "./invitations";

// Takes an event offered for swap: verifies `onSwap` and reassigns it in a transaction.
export { takeEvent } from "./events";

// Renames a user everywhere their name is copied: the client may not write it itself.
export { updateProfile } from "./users";

// Notifies members when an event is put up for swap.
export { onEventPutOnSwap } from "./notifications";

// On-demand aggregation of another user's full calendar (cross-group).
export { getSharedCalendar } from "./sharedCalendar";
