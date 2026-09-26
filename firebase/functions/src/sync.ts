import {
  DocumentData,
  DocumentReference,
  FieldValue,
  Firestore,
  SetOptions,
} from "firebase-admin/firestore";

/**
 * Every write to a `sync/updates` marker, on users and on groups alike.
 *
 * A marker is only worth something if it moves in the **same commit** as the document it gates: a
 * batch or a transaction resolves every server timestamp in it to one instant, so a reader sees both
 * as equally old. Written apart, the marker is always the later of the two and no cache ever settles.
 * That is why nothing here commits on its own — each function only adds a write to the caller's.
 *
 * Each writer merges only its own field, so markers never overwrite each other.
 */

/** A `WriteBatch` or a `Transaction`: both take the marker as one more write of their commit. */
export interface SyncWriter {
  set(ref: DocumentReference, data: DocumentData, options: SetOptions): unknown;
}

/**
 * The markers on `users/{uid}/sync/updates`, one per document they gate.
 *
 * `private` is missing on purpose: it is the legacy marker `account` and `joinRequests` used to share,
 * and it is never written again.
 */
export type UserMarker =
  | "account"
  | "profile"
  | "joinRequests"
  | "preferences"
  // Server-only, and the rules keep it that way: the receipt-verification function commits it with
  // `private/subscription`, and a client able to move it could pin a cached premium past a refund.
  | "subscription"
  | "revokedGroups"
  | "personalEventsUpdatedAt"
  | "personalEventTypesUpdatedAt";

export function markUserUpdated(db: Firestore, writer: SyncWriter, uid: string, marker: UserMarker) {
  writer.set(
    db.doc(`users/${uid}/sync/updates`),
    { [marker]: FieldValue.serverTimestamp() },
    { merge: true },
  );
}

/**
 * The user became a member of the group. `users/{uid}/sync/updates.groups` is the membership list
 * the app follows instead of a `memberUids` query listener, which bills every group again on each
 * re-attach. Added unconditionally: the app ignores the map until it has indexed the rest itself.
 */
export function markGroupJoined(db: Firestore, writer: SyncWriter, uid: string, groupId: string) {
  writer.set(
    db.doc(`users/${uid}/sync/updates`),
    { groups: { [groupId]: FieldValue.serverTimestamp() } },
    { merge: true },
  );
}

/** The user is no longer a member: they left, were removed, or the group is gone. */
export function markGroupLeft(db: Firestore, writer: SyncWriter, uid: string, groupId: string) {
  writer.set(
    db.doc(`users/${uid}/sync/updates`),
    { groups: { [groupId]: FieldValue.delete() } },
    { merge: true },
  );
}

/** The group document moved: its name, invitation, event types or member roster. */
export function markGroupUpdated(db: Firestore, writer: SyncWriter, groupId: string) {
  writer.set(
    db.doc(`groups/${groupId}/sync/updates`),
    { group: FieldValue.serverTimestamp() },
    { merge: true },
  );
}

/**
 * A request to join the group is waiting for an admin, keyed by requester and stamped with the
 * request's own `requestedAt` — the same commit, so the same instant. The map is the whole pending
 * list: an admin reads the requests it names, cache first, instead of querying the collection.
 */
export function markJoinRequestPending(
  db: Firestore,
  writer: SyncWriter,
  groupId: string,
  uid: string,
) {
  writer.set(
    db.doc(`groups/${groupId}/sync/updates`),
    { joinRequests: { [uid]: FieldValue.serverTimestamp() } },
    { merge: true },
  );
}

/**
 * The request is no longer waiting: answered, or withdrawn with the account. Removing the key is what
 * tells an admin's cache — a deleted document never shows up in a query that asks what changed.
 */
export function markJoinRequestSettled(
  db: Firestore,
  writer: SyncWriter,
  groupId: string,
  uid: string,
) {
  writer.set(
    db.doc(`groups/${groupId}/sync/updates`),
    { joinRequests: { [uid]: FieldValue.delete() } },
    { merge: true },
  );
}

/** An event of the given `YYYY-MM` moved. The merge is deep, so other months keep their own. */
export function markGroupEventsUpdated(
  db: Firestore,
  writer: SyncWriter,
  groupId: string,
  yearMonth: string,
) {
  writer.set(
    db.doc(`groups/${groupId}/sync/updates`),
    { events: { [yearMonth]: { updatedAt: FieldValue.serverTimestamp() } } },
    { merge: true },
  );
}

/**
 * A shift the user holds, or has just stopped holding, moved in the given `YYYY-MM`. Kept on the
 * user's own sync document because that is the one a colleague the calendar is shared with can
 * read: the group's own marker is for members only.
 */
export function markUserGroupEventsUpdated(
  db: Firestore,
  writer: SyncWriter,
  uid: string,
  yearMonth: string,
) {
  writer.set(
    db.doc(`users/${uid}/sync/updates`),
    { groupEvents: { [yearMonth]: { updatedAt: FieldValue.serverTimestamp() } } },
    { merge: true },
  );
}
