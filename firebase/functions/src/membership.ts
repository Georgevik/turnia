import { onCall } from "firebase-functions/v2/https";
import {
  DocumentSnapshot,
  FieldValue,
  Firestore,
  WriteBatch,
  getFirestore,
} from "firebase-admin/firestore";
import {
  HttpErrorFailedPrecondition,
  HttpErrorInvalidArgument,
  HttpErrorPermissionDenied,
  HttpErrorUnauthenticated,
  TurniaErrorCode,
} from "./errors";

function markRevokedGroupsUpdated(db: Firestore, batch: WriteBatch, uid: string) {
  batch.set(
    db.doc(`users/${uid}/sync/updates`),
    { revokedGroups: FieldValue.serverTimestamp() },
    { merge: true },
  );
}

export function clearRevokedGroup(
  db: Firestore,
  batch: WriteBatch,
  group: DocumentSnapshot,
  uid: string,
) {
  const revokedUids = (group.get("revokedUids") as string[] | undefined) ?? [];
  if (!revokedUids.includes(uid)) return;

  batch.set(
    db.doc(`users/${uid}/revokedGroups/${group.id}`),
    { isDeleted: true, updateAt: FieldValue.serverTimestamp() },
    { merge: true },
  );
  markRevokedGroupsUpdated(db, batch, uid);
}

/**
 * Withdraws someone's membership, keeping whatever they still have to cover.
 *
 * Their events stay in the group — the remaining members still need to know who was meant to work
 * those days — so the person keeps a narrower kind of access instead of losing everything: their uid
 * moves from `memberUids` to `revokedUids`, and the security rules then let them read only the
 * events where `assigneeId` is their own.
 *
 * They can no longer read the group document itself, which carries the member roster and the
 * invitation code, so the little they still need to render those events is snapshotted onto
 * `users/{uid}/revokedGroups/{groupId}` — the group's name, and only the event types their leftover
 * events actually use.
 *
 * Someone who leaves nothing behind is worth no tombstone at all: they are dropped from `memberUids`
 * and `members` and that is the end of it, with no `revokedUids` entry and no snapshot.
 */
async function revoke(db: Firestore, groupId: string, uid: string) {
  const groupRef = db.doc(`groups/${groupId}`);
  const group = await groupRef.get();

  const events = await groupRef.collection("events").where("assigneeId", "==", uid).get();
  const held = events.docs.filter((doc) => doc.get("isDeleted") !== true);

  const batch = db.batch();
  batch.update(groupRef, {
    memberUids: FieldValue.arrayRemove(uid),
    adminUids: FieldValue.arrayRemove(uid),
    [`members.${uid}`]: FieldValue.delete(),
    ...(held.length > 0 ? { revokedUids: FieldValue.arrayUnion(uid) } : {}),
    updateAt: FieldValue.serverTimestamp(),
  });

  if (held.length > 0) {
    const usedTypeIds = new Set(held.map((doc) => doc.get("groupEventTypeId") as string));
    const groupEventTypes = ((group.get("groupEventTypes") as Record<string, unknown>[]) ?? [])
      .filter((type) => usedTypeIds.has(type.id as string));

    batch.set(db.doc(`users/${uid}/revokedGroups/${groupId}`), {
      name: group.get("name") ?? "",
      groupEventTypes,
      revokedAt: FieldValue.serverTimestamp(),
      isDeleted: false,
      updateAt: FieldValue.serverTimestamp(),
    });
    markRevokedGroupsUpdated(db, batch, uid);
  }

  // Same commit as the group, so both resolve to one instant and a reader's cache can settle.
  batch.set(
    db.doc(`groups/${groupId}/sync/updates`),
    { group: FieldValue.serverTimestamp() },
    { merge: true },
  );
  await batch.commit();

  return held.length > 0 ? ("revoked" as const) : ("removed" as const);
}

/**
 * Leaves a group. The client never writes `memberUids`, so this is the only way out.
 *
 * The last admin of a group that still has other members is refused: there is no way to promote
 * anyone yet, so letting them go would leave a group nobody can administer.
 *
 * Request data: `{ groupId: string }`
 * Returns: `{ groupId: string, status: "revoked" | "removed" }`
 */
export const leaveGroup = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpErrorUnauthenticated(TurniaErrorCode.LeaveGroupUnauthenticated, "Sign in required.");
  }

  const groupId = request.data?.groupId as string | undefined;
  if (!groupId) {
    throw new HttpErrorInvalidArgument(TurniaErrorCode.LeaveGroupMissingArgs, "Missing groupId.");
  }

  const db = getFirestore();
  const group = await db.doc(`groups/${groupId}`).get();
  const memberUids = (group.get("memberUids") as string[] | undefined) ?? [];
  if (!memberUids.includes(uid)) {
    throw new HttpErrorFailedPrecondition(TurniaErrorCode.LeaveGroupNotMember, "You are not a member of this group.");
  }

  const adminUids = (group.get("adminUids") as string[] | undefined) ?? [];
  if (adminUids.length === 1 && adminUids[0] === uid && memberUids.length > 1) {
    throw new HttpErrorFailedPrecondition(
      TurniaErrorCode.LeaveGroupLastAdmin,
      "The only admin cannot leave a group that still has members.",
    );
  }

  const status = await revoke(db, groupId, uid);
  return { groupId, status };
});

/**
 * Removes a member. Admin-only, and the mirror image of `leaveGroup`.
 *
 * An admin cannot remove another admin — that is a demotion, which does not exist — and cannot
 * remove themselves, which is `leaveGroup` and has its own last-admin check.
 *
 * Request data: `{ groupId: string, uid: string }`
 * Returns: `{ groupId: string, uid: string, status: "revoked" | "removed" }`
 */
export const removeMember = onCall(async (request) => {
  const adminUid = request.auth?.uid;
  if (!adminUid) {
    throw new HttpErrorUnauthenticated(TurniaErrorCode.RemoveMemberUnauthenticated, "Sign in required.");
  }

  const groupId = request.data?.groupId as string | undefined;
  const uid = request.data?.uid as string | undefined;
  if (!groupId || !uid) {
    throw new HttpErrorInvalidArgument(TurniaErrorCode.RemoveMemberMissingArgs, "Missing groupId or uid.");
  }

  const db = getFirestore();
  const group = await db.doc(`groups/${groupId}`).get();
  const adminUids = (group.get("adminUids") as string[] | undefined) ?? [];
  if (!adminUids.includes(adminUid)) {
    throw new HttpErrorPermissionDenied(TurniaErrorCode.RemoveMemberNotAdmin, "Only a group admin can remove members.");
  }
  if (uid === adminUid) {
    throw new HttpErrorFailedPrecondition(TurniaErrorCode.RemoveMemberSelf, "Use leaveGroup to leave a group.");
  }
  if (adminUids.includes(uid)) {
    throw new HttpErrorFailedPrecondition(TurniaErrorCode.RemoveMemberIsAdmin, "An admin cannot be removed.");
  }

  const memberUids = (group.get("memberUids") as string[] | undefined) ?? [];
  if (!memberUids.includes(uid)) {
    throw new HttpErrorFailedPrecondition(TurniaErrorCode.RemoveMemberNotMember, "That user is not a member of this group.");
  }

  const status = await revoke(db, groupId, uid);
  return { groupId, uid, status };
});
