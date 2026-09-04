import { onCall } from "firebase-functions/v2/https";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import {
  HttpErrorFailedPrecondition,
  HttpErrorInvalidArgument,
  HttpErrorNotFound,
  HttpErrorPermissionDenied,
  HttpErrorUnauthenticated,
  TurniaErrorCode,
} from "./errors";

/**
 * Requests to join a group by validating its (single) invitation code.
 *
 * Anyone with the code can request access; this creates a pending
 * `groups/{groupId}/joinRequests/{uid}` doc. A group admin then accepts it with
 * `acceptJoinRequest`. The client never writes `memberUids` directly.
 *
 * Request data: `{ code: string }`
 * Returns: `{ groupId: string, status: "already_member" | "requested" }`
 */
export const requestToJoinGroup = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpErrorUnauthenticated(TurniaErrorCode.JoinRequestUnauthenticated, "Sign in required.");
  }

  const code = (request.data?.code as string | undefined)?.trim();
  if (!code) {
    throw new HttpErrorInvalidArgument(TurniaErrorCode.JoinRequestMissingCode, "Missing invitation code.");
  }

  const db = getFirestore();
  const groups = await db
    .collection("groups")
    .where("invitation.code", "==", code)
    .limit(1)
    .get();

  if (groups.empty) {
    throw new HttpErrorNotFound(TurniaErrorCode.JoinRequestInvitationNotFound, "Invalid invitation code.");
  }

  const groupDoc = groups.docs[0];
  const invitation = groupDoc.get("invitation") ?? {};

  if (invitation.active !== true) {
    throw new HttpErrorFailedPrecondition(TurniaErrorCode.JoinRequestInvitationNotActive, "Invitation is not active.");
  }
  const expiresAt = invitation.expiresAt;
  if (expiresAt && typeof expiresAt.toMillis === "function" && expiresAt.toMillis() < Date.now()) {
    throw new HttpErrorFailedPrecondition(TurniaErrorCode.JoinRequestInvitationExpired, "Invitation has expired.");
  }

  const memberUids = (groupDoc.get("memberUids") as string[] | undefined) ?? [];
  if (memberUids.includes(uid)) {
    return { groupId: groupDoc.id, status: "already_member" as const };
  }

  await groupDoc.ref.collection("joinRequests").doc(uid).set({
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
  const adminUid = request.auth?.uid;
  if (!adminUid) {
    throw new HttpErrorUnauthenticated(TurniaErrorCode.AcceptRequestUnauthenticated, "Sign in required.");
  }

  const groupId = request.data?.groupId as string | undefined;
  const uid = request.data?.uid as string | undefined;
  if (!groupId || !uid) {
    throw new HttpErrorInvalidArgument(TurniaErrorCode.AcceptRequestMissingArgs, "Missing groupId or uid.");
  }

  const db = getFirestore();
  const groupRef = db.doc(`groups/${groupId}`);
  const group = await groupRef.get();
  const adminUids = (group.get("adminUids") as string[] | undefined) ?? [];
  if (!adminUids.includes(adminUid)) {
    throw new HttpErrorPermissionDenied(TurniaErrorCode.AcceptRequestNotAdmin, "Only a group admin can accept requests.");
  }

  const requestRef = db.doc(`groups/${groupId}/joinRequests/${uid}`);
  const requestSnap = await requestRef.get();
  if (!requestSnap.exists) {
    throw new HttpErrorNotFound(TurniaErrorCode.AcceptRequestNotFound, "No pending join request.");
  }

  // Membership is a field on the group, so joining is an arrayUnion: two admins accepting at the
  // same time add their own requester instead of overwriting each other's list.
  const batch = db.batch();
  batch.update(groupRef, {
    memberUids: FieldValue.arrayUnion(uid),
    updateAt: FieldValue.serverTimestamp(),
  });
  batch.delete(requestRef);
  await batch.commit();

  return { groupId, uid, status: "accepted" as const };
});
