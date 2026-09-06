import { onCall } from "firebase-functions/v2/https";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { clearRevokedGroup } from "./membership";
import { TurniaError } from "./errors";
import { requireFields, requireUid } from "./requests";

/**
 * Requests to join a group by validating its (single) invitation code.
 *
 * With `invitation.autoApprove` the code is the door: the requester is added to the group here and
 * now. Otherwise this creates a pending `groups/{groupId}/joinRequests/{uid}` doc that an admin
 * accepts with `acceptJoinRequest`. The client never writes `memberUids` directly.
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
    batch.set(
      db.doc(`groups/${groupDoc.id}/sync/updates`),
      { group: FieldValue.serverTimestamp() },
      { merge: true },
    );
    await batch.commit();

    return { groupId: groupDoc.id, status: "joined" as const };
  }

  await groupDoc.ref.collection("joinRequests").doc(uid).set({
    ...profile,
    requestedAt: FieldValue.serverTimestamp(),
  });

  return { groupId: groupDoc.id, status: "requested" as const };
});

/**
 * Accepts a pending join request. Admin-only: adds the requester to the group's
 * `memberUids` and removes the request atomically.
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
  batch.set(
    db.doc(`groups/${groupId}/sync/updates`),
    { group: FieldValue.serverTimestamp() },
    { merge: true },
  );
  batch.delete(requestRef);
  await batch.commit();

  return { groupId, uid, status: "accepted" as const };
});
