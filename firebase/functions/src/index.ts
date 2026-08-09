import { initializeApp } from "firebase-admin/app";

initializeApp();

// Group membership: request to join (validates the invitation) and admin acceptance.
export { requestToJoinGroup, acceptJoinRequest } from "./invitations";

// Takes an on-sale event: cross-member move + history copy-forward.
export { takeEvent } from "./events";

// Notifies members when an event is put on sale.
export { onEventPutOnSale } from "./notifications";

// On-demand aggregation of another user's full calendar (cross-group).
export { getSharedCalendar } from "./sharedCalendar";
