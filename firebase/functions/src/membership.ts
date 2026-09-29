import { onCall } from "firebase-functions/v2/https";
import {
  DocumentSnapshot,
  FieldValue,
  Firestore,
  WriteBatch,
  getFirestore,
} from "firebase-admin/firestore";
import { TurniaError } from "./errors";
import { requireFields, requireUid } from "./requests";
import { markGroupLeft, markGroupUpdated, markUserUpdated } from "./sync";
import { writeJoinRequestPointer } from "./users";

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
  markUserUpdated(db, batch, uid, "revokedGroups");
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
 * `users/{uid}/revokedGroups/{groupId}` — the group's name and colour, and only the event types
 * their leftover events actually use.
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
      color: group.get("color") ?? null,
      groupEventTypes,
      revokedAt: FieldValue.serverTimestamp(),
      isDeleted: false,
      updateAt: FieldValue.serverTimestamp(),
    });
    markUserUpdated(db, batch, uid, "revokedGroups");
  }

  // Same commit as the group, so both resolve to one instant and a reader's cache can settle.
  markGroupUpdated(db, batch, groupId);
  markGroupLeft(db, batch, uid, groupId);
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
export const leaveGroup = onCall({ enforceAppCheck: true }, async (request) => {
  const uid = requireUid(request);
  const { groupId } = requireFields(request, "groupId");

  const db = getFirestore();
  const group = await db.doc(`groups/${groupId}`).get();
  const memberUids = (group.get("memberUids") as string[] | undefined) ?? [];
  if (!memberUids.includes(uid)) {
    throw TurniaError.LeaveGroupNotMember;
  }

  if (isLastAdminOfOthers(group, uid)) {
    throw TurniaError.LeaveGroupLastAdmin;
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
export const removeMember = onCall({ enforceAppCheck: true }, async (request) => {
  const adminUid = requireUid(request);
  const { groupId, uid } = requireFields(request, "groupId", "uid");

  const db = getFirestore();
  const group = await db.doc(`groups/${groupId}`).get();
  const adminUids = (group.get("adminUids") as string[] | undefined) ?? [];
  if (!adminUids.includes(adminUid)) {
    throw TurniaError.RemoveMemberNotAdmin;
  }
  if (uid === adminUid) {
    throw TurniaError.RemoveMemberSelf;
  }
  if (adminUids.includes(uid)) {
    throw TurniaError.RemoveMemberIsAdmin;
  }

  const memberUids = (group.get("memberUids") as string[] | undefined) ?? [];
  if (!memberUids.includes(uid)) {
    throw TurniaError.RemoveMemberNotMember;
  }

  const status = await revoke(db, groupId, uid);
  return { groupId, uid, status };
});

/**
 * Deletes a group, once its admin is the only one left in it.
 *
 * Server-only, and not because of who is allowed to: a client deleting `groups/{groupId}` would
 * delete one document and leave `events`, `sync` and `joinRequests` behind, since Firestore does
 * not cascade into subcollections. Those orphans are unreachable — every rule that guards them
 * looks the group up first — and still stored and billed. `recursiveDelete` is what actually
 * empties the tree.
 *
 * The last-member condition is the whole safety story here: there is no undo, and nobody else can
 * be surprised by it, because by the time it is allowed there is nobody else left. Anyone still in
 * the group has to leave, or be removed, first.
 *
 * Request data: `{ groupId: string }`
 * Returns: `{ groupId: string, status: "deleted" }`
 */
export const deleteGroup = onCall({ enforceAppCheck: true }, async (request) => {
  const uid = requireUid(request);
  const { groupId } = requireFields(request, "groupId");

  const db = getFirestore();
  const groupRef = db.doc(`groups/${groupId}`);
  const group = await groupRef.get();
  if (!group.exists) {
    throw TurniaError.DeleteGroupNotFound;
  }

  const adminUids = (group.get("adminUids") as string[] | undefined) ?? [];
  if (!adminUids.includes(uid)) {
    throw TurniaError.DeleteGroupNotAdmin;
  }

  const memberUids = (group.get("memberUids") as string[] | undefined) ?? [];
  if (memberUids.some((member) => member !== uid)) {
    throw TurniaError.DeleteGroupNotEmpty;
  }

  await deleteGroupTree(db, group);

  return { groupId, status: "deleted" as const };
});

/**
 * Empties a group's whole tree and tells everyone outside it who still points at it.
 *
 * Shared by `deleteGroup` and `deleteAccount`, which reaches it for every group the account is the
 * last member of. Neither checks anything here: by the time this runs, the caller has established
 * that nobody else is left to be surprised.
 */
export async function deleteGroupTree(db: Firestore, group: DocumentSnapshot) {
  const groupId = group.id;

  // Someone removed earlier may still be holding a snapshot of this group to render the shifts
  // they were left with. Those shifts go with the group, so the snapshot is tombstoned.
  const revokedUids = (group.get("revokedUids") as string[] | undefined) ?? [];

  // Anyone still waiting to be let in keeps a pointer to this group under their own private
  // document, and `recursiveDelete` cannot reach it: it is not under the group. Read them while
  // the requests still exist.
  const requests = await group.ref.collection("joinRequests").get();
  const memberUids = (group.get("memberUids") as string[] | undefined) ?? [];

  // The group first: a tombstone written before a delete that then fails would take a group away
  // from people it still exists for. The other way round they keep a stale, empty entry at worst.
  await db.recursiveDelete(group.ref);

  if (revokedUids.length > 0 || !requests.empty || memberUids.length > 0) {
    const batch = db.batch();
    for (const memberUid of memberUids) {
      markGroupLeft(db, batch, memberUid, groupId);
    }
    for (const revokedUid of revokedUids) {
      batch.set(
        db.doc(`users/${revokedUid}/revokedGroups/${groupId}`),
        { isDeleted: true, updateAt: FieldValue.serverTimestamp() },
        { merge: true },
      );
      markUserUpdated(db, batch, revokedUid, "revokedGroups");
    }
    for (const requestDoc of requests.docs) {
      writeJoinRequestPointer(db, batch, requestDoc.id, FieldValue.arrayRemove(groupId));
    }
    await batch.commit();
  }
}

/** Whether leaving would strand a group with members and nobody to administer it. */
export function isLastAdminOfOthers(group: DocumentSnapshot, uid: string): boolean {
  const memberUids = (group.get("memberUids") as string[] | undefined) ?? [];
  const adminUids = (group.get("adminUids") as string[] | undefined) ?? [];
  return adminUids.length === 1 && adminUids[0] === uid && memberUids.length > 1;
}
