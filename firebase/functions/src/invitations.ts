import { onCall } from "firebase-functions/v2/https";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { clearRevokedGroup } from "./membership";
import { notifyJoinAccepted, notifyJoinRequested } from "./notifications";
import { TurniaError } from "./errors";
import { requireFields, requireUid } from "./requests";
import {
  markGroupJoined,
  markGroupUpdated,
  markJoinRequestPending,
  markJoinRequestSettled,
  markUserUpdated,
} from "./sync";
import { writeJoinRequestPointer } from "./users";

/**
 * Requests to join a group by validating its (single) invitation code.
 *
 * With `invitation.autoApprove` the code is the door: the requester is added to the group here and
 * now. Otherwise this creates a pending `groups/{groupId}/joinRequests/{uid}` doc that an admin
 * answers with `acceptJoinRequest` / `rejectJoinRequest`. The client never writes `memberUids`
 * directly.
 *
 * Request data: `{ code: string }`
 * Returns: `{ groupId: string, status: "already_member" | "joined" | "requested" }`
 */
export const requestToJoinGroup = onCall(async (request) => {
  const uid = requireUid(request);
  const { code } = requireFields(request, "code");

  const db = getFirestore();
  const groups = await db
    .collection("groups")
    .where("invitation.code", "==", code)
    .limit(1)
    .get();

  if (groups.empty) {
    throw TurniaError.JoinRequestInvitationNotFound;
  }

  const groupDoc = groups.docs[0];
  const invitation = groupDoc.get("invitation") ?? {};

  if (invitation.active !== true) {
    throw TurniaError.JoinRequestInvitationNotActive;
  }
  const expiresAt = invitation.expiresAt;
  if (expiresAt && typeof expiresAt.toMillis === "function" && expiresAt.toMillis() < Date.now()) {
    throw TurniaError.JoinRequestInvitationExpired;
  }

  const memberUids = (groupDoc.get("memberUids") as string[] | undefined) ?? [];
  if (memberUids.includes(uid)) {
    return { groupId: groupDoc.id, status: "already_member" as const };
  }

  // The name travels with the request: `users/{uid}` is unreadable to an admin who does not share
  // a calendar with the requester, so without it there is nobody to show on the approval screen.
  const user = await db.doc(`users/${uid}`).get();
  const profile = {
    name: (user.get("name") as string | undefined) ?? "",
    username: (user.get("username") as string | undefined) ?? "",
  };

  if (invitation.autoApprove === true) {
    // Same commit as the group, so both resolve to one instant and a reader's cache can settle.
    const batch = db.batch();
    batch.update(groupDoc.ref, {
      memberUids: FieldValue.arrayUnion(uid),
      // Rejoining undoes a revocation: a uid is a member or revoked, never both.
      revokedUids: FieldValue.arrayRemove(uid),
      [`members.${uid}`]: profile,
      updateAt: FieldValue.serverTimestamp(),
    });
    clearRevokedGroup(db, batch, groupDoc, uid);
    markGroupUpdated(db, batch, groupDoc.id);
    markGroupJoined(db, batch, uid, groupDoc.id);
    await batch.commit();

    return { groupId: groupDoc.id, status: "joined" as const };
  }

  // The request and the requester's pointer to it in one commit: the pointer list is the only way
  // they can find this document again — the rules let them read it by id, and no query can express
  // that — so a pointer without a request, or a request nobody can reach, is worse than neither.
  // `set` and not `create`: asking again after a rejection flips that receipt back to pending.
  const batch = db.batch();
  batch.set(groupDoc.ref.collection("joinRequests").doc(uid), {
    ...profile,
    groupName: groupDoc.get("name") ?? "",
    status: "pending",
    respondedAt: null,
    requestedAt: FieldValue.serverTimestamp(),
  });
  writeJoinRequestPointer(db, batch, uid, FieldValue.arrayUnion(groupDoc.id));
  markJoinRequestPending(db, batch, groupDoc.id, uid);
  await batch.commit();
  // After the write: the request is what the admins are being told about, and a push about one
  // that failed to save would send them to an approval screen with nothing on it.
  await notifyJoinRequested(groupDoc, profile);

  return { groupId: groupDoc.id, status: "requested" as const };
});

/**
 * Accepts a pending join request. Admin-only: adds the requester to the group's `memberUids` and
 * answers the request atomically.
 *
 * The request document is answered rather than deleted. It is the only thing the requester can read
 * to learn the outcome — they cannot query for it, and a deleted document says nothing — so it stays
 * as a receipt until their app acknowledges it.
 *
 * Request data: `{ groupId: string, uid: string }`
 * Returns: `{ groupId: string, uid: string, status: "accepted" }`
 */
export const acceptJoinRequest = onCall(async (request) => {
  const adminUid = requireUid(request);
  const { groupId, uid } = requireFields(request, "groupId", "uid");

  const db = getFirestore();
  const groupRef = db.doc(`groups/${groupId}`);
  const group = await groupRef.get();
  const adminUids = (group.get("adminUids") as string[] | undefined) ?? [];
  if (!adminUids.includes(adminUid)) {
    throw TurniaError.AcceptRequestNotAdmin;
  }

  const requestRef = db.doc(`groups/${groupId}/joinRequests/${uid}`);
  const requestSnap = await requestRef.get();
  if (!requestSnap.exists) {
    throw TurniaError.AcceptRequestNotFound;
  }

  // The name travels with the membership: showing who covers a shift then costs no read at all.
  const user = await db.doc(`users/${uid}`).get();

  // Membership is a field on the group, so joining is an arrayUnion and the member entry is a
  // dotted path: two admins accepting at the same time add their own requester instead of
  // overwriting each other's work.
  const batch = db.batch();
  batch.update(groupRef, {
    memberUids: FieldValue.arrayUnion(uid),
    // Rejoining undoes a revocation: a uid is a member or revoked, never both.
    revokedUids: FieldValue.arrayRemove(uid),
    [`members.${uid}`]: {
      name: user.get("name") ?? "",
      username: user.get("username") ?? "",
    },
    updateAt: FieldValue.serverTimestamp(),
  });
  clearRevokedGroup(db, batch, group, uid);
  // Same commit as the group, so both resolve to one instant and a reader's cache can settle.
  markGroupUpdated(db, batch, groupId);
  markGroupJoined(db, batch, uid, groupId);
  batch.update(requestRef, { status: "accepted", respondedAt: FieldValue.serverTimestamp() });
  markUserUpdated(db, batch, uid, "joinRequests");
  markJoinRequestSettled(db, batch, groupId, uid);
  await batch.commit();

  await notifyJoinAccepted(groupId, group.get("name") ?? "", uid);

  return { groupId, uid, status: "accepted" as const };
});

/**
 * Rejects a pending join request. Admin-only, and the mirror image of `acceptJoinRequest` minus
 * everything about the group: nothing joins, so the group document and its `group` marker stay put.
 * Only the request leaves the pending map.
 *
 * Server-only because `status` is frozen against every client by the rules, admins included. It used
 * to be the admin deleting the request document, which answered it by destroying the only thing that
 * could carry the answer.
 *
 * Request data: `{ groupId: string, uid: string }`
 * Returns: `{ groupId: string, uid: string, status: "rejected" }`
 */
export const rejectJoinRequest = onCall(async (request) => {
  const adminUid = requireUid(request);
  const { groupId, uid } = requireFields(request, "groupId", "uid");

  const db = getFirestore();
  const group = await db.doc(`groups/${groupId}`).get();
  const adminUids = (group.get("adminUids") as string[] | undefined) ?? [];
  if (!adminUids.includes(adminUid)) {
    throw TurniaError.RejectRequestNotAdmin;
  }

  const requestRef = db.doc(`groups/${groupId}/joinRequests/${uid}`);
  const requestSnap = await requestRef.get();
  if (!requestSnap.exists) {
    throw TurniaError.RejectRequestNotFound;
  }

  const batch = db.batch();
  batch.update(requestRef, { status: "rejected", respondedAt: FieldValue.serverTimestamp() });
  markUserUpdated(db, batch, uid, "joinRequests");
  markJoinRequestSettled(db, batch, groupId, uid);
  await batch.commit();

  return { groupId, uid, status: "rejected" as const };
});
